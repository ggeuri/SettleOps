package com.settleops.domain.hold.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.settleops.domain.hold.domain.Hold;
import com.settleops.domain.hold.domain.HoldEvent;
import com.settleops.domain.hold.domain.HoldStatus;
import com.settleops.domain.hold.infra.HoldEventRepository;
import com.settleops.domain.hold.infra.HoldRepository;
import com.settleops.domain.settlement.infra.SettlementRepository;
import com.settleops.global.audit.ActorType;
import com.settleops.global.audit.AuditLogCommand;
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.audit.EntityType;
import com.settleops.global.enums.Action;
import com.settleops.global.enums.ReasonCode;
import com.settleops.global.error.ConflictException;
import com.settleops.global.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class HoldServiceImpl implements HoldService {

    private final HoldRepository holdRepository;
    private final HoldEventRepository holdEventRepository;
    private final SettlementRepository settlementRepository;
    private final AuditLogger auditLogger;
    private final ObjectMapper objectMapper;

    @Override
    public HoldCreateResponse createHold(HoldCreateCommand command) {

        //	2.	settlementRepository.findById(command.settlementId())
        var settlement = settlementRepository.findById(command.settlementId())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 settlement 입니다."));

        //	3.	settlement가 PAID면 PAID_ALREADY
        if (settlement.isPaid()) {
            throw new ConflictException(ReasonCode.PAID_ALREADY, "이미 PAID 상태인 settlement 입니다.");
        }
        //	4.	holdRepository.existsBySettlementId(...)면 HOLD_ALREADY_EXISTS

        if (holdRepository.existsBySettlementId(command.settlementId())) {
            throw new ConflictException(ReasonCode.HOLD_ALREADY_EXISTS, "이미 hold가 존재하는 settlement 입니다.");
        }
        //	5.	Hold.requested(...) 생성
        var hold = Hold.requested(command.settlementId(), command.reasonCode(), command.comment(), command.actorId());

        //	6.	holdRepository.save(hold)
        var savedHold = holdRepository.save(hold);

        String metaJson = buildMetaJson(
                command.comment(),
                command.reasonCode().name(),
                null,
                savedHold.getStatus().name(),
                false,
                null
        );

        var holdEvent = HoldEvent.of(
                savedHold.getHoldId(),
                Action.HOLD_REQUESTED,
                command.requestId(),
                ActorType.ADMIN,
                command.actorId(),
                savedHold.getCreatedAt(),
                metaJson
        );

        holdEventRepository.save(holdEvent);

        //	7.	auditLogger.log(...)
        auditLogger.log(AuditLogCommand.builder().requestId(command.requestId())
                .merchantId(settlement.getMerchantId()).entityType(EntityType.HOLD).occurredAt(savedHold.getCreatedAt())
                .actorType(ActorType.ADMIN).actorId(command.actorId()).action(Action.HOLD_REQUESTED)
                .statusBefore(null).statusAfter(savedHold.getStatus().name()).entityId(savedHold.getHoldId())
                .metaJson(metaJson).build());

        //	8.	HoldCreateResponse 반환

        return new HoldCreateResponse(
                command.requestId(),
                savedHold.getHoldId(),
                savedHold.getSettlementId(),
                savedHold.getStatus(),
                savedHold.getCreatedAt()
        );
    }

    @Override
    public HoldDecisionResponse approveHold(String holdId, HoldApproveCommand command) {

        //1.	hold 조회
        Hold hold = holdRepository.findById(holdId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 hold 입니다."));
        //	2.	settlement 조회
        var settlement = settlementRepository.findById(hold.getSettlementId())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 settlement 입니다."));
        //	3.	hold 상태 확인
        HoldStatus beforeStatus = hold.getStatus();
        LocalDateTime now = LocalDateTime.now();


        //	4.	HOLD_ACTIVE면 no-op 200 + audit
        if (beforeStatus == HoldStatus.HOLD_ACTIVE) {

            String metaJson = buildMetaJson(
                    command.comment(),
                    hold.getRequestedReasonCode().name(),
                    beforeStatus.name(),
                    beforeStatus.name(),
                    true,
                    "ALREADY_APPROVED"
            );

            auditLogger.log(AuditLogCommand.builder()
                    .requestId(command.requestId())
                    .merchantId(settlement.getMerchantId())
                    .entityType(EntityType.HOLD)
                    .occurredAt(now)
                    .actorType(ActorType.ADMIN)
                    .actorId(command.actorId())
                    .action(Action.HOLD_APPROVED)
                    .statusBefore(beforeStatus.name())
                    .statusAfter(beforeStatus.name())
                    .entityId(hold.getHoldId())
                    .metaJson(metaJson)
                    .build());


            return new HoldDecisionResponse(
                    command.requestId(),
                    hold.getStatus(),
                    now
            );
        }

        //	5.	RELEASED면 no-op 200 + audit
        if (beforeStatus == HoldStatus.RELEASED) {
            String metaJson = buildMetaJson(
                    command.comment(),
                    hold.getRequestedReasonCode().name(),
                    beforeStatus.name(),
                    beforeStatus.name(),
                    true,
                    "ALREADY_RELEASED"
            );

            auditLogger.log(AuditLogCommand.builder()
                    .requestId(command.requestId())
                    .merchantId(settlement.getMerchantId())
                    .entityType(EntityType.HOLD)
                    .entityId(hold.getHoldId())
                    .occurredAt(now)
                    .actorType(ActorType.ADMIN)
                    .actorId(command.actorId())
                    .action(Action.HOLD_APPROVED)
                    .statusBefore(beforeStatus.name())
                    .statusAfter(beforeStatus.name())
                    .metaJson(metaJson)
                    .build());

            return new HoldDecisionResponse(
                    command.requestId(),
                    hold.getStatus(),
                    now
            );

        }
        //	6.	HOLD_REQUESTED면 hold.approve()
        if (beforeStatus == HoldStatus.HOLD_REQUESTED) {
            String metaJson = buildMetaJson(
                    command.comment(),
                    hold.getRequestedReasonCode().name(),
                    beforeStatus.name(),
                    HoldStatus.HOLD_ACTIVE.name(),
                    false,
                    null
            );

            hold.approve();
            //	7.	settlement markHoldActive()
            settlement.markHoldActive();
            //	8.	audit 저장
            holdEventRepository.save(HoldEvent.of(
                    hold.getHoldId(),
                    Action.HOLD_APPROVED,
                    command.requestId(),
                    ActorType.ADMIN,
                    command.actorId(),
                    now,
                    metaJson
            ));

            auditLogger.log(AuditLogCommand.builder()
                    .requestId(command.requestId())
                    .merchantId(settlement.getMerchantId())
                    .entityType(EntityType.HOLD)
                    .entityId(hold.getHoldId())
                    .occurredAt(now)
                    .actorType(ActorType.ADMIN)
                    .actorId(command.actorId())
                    .action(Action.HOLD_APPROVED)
                    .statusBefore(beforeStatus.name())
                    .statusAfter(HoldStatus.HOLD_ACTIVE.name())
                    .metaJson(metaJson)
                    .build());


            //	9.	HoldDecisionResponse 반환

            return new HoldDecisionResponse(
                    command.requestId(),
                    HoldStatus.HOLD_ACTIVE,
                    now
            );
        }
        throw new IllegalStateException("unsupported hold status: " + beforeStatus);
    }

    @Override
    public HoldDecisionResponse releaseHold(String holdId, HoldReleaseCommand command) {

        //1.	hold 조회
        Hold hold = holdRepository.findById(holdId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 hold 입니다."));
        //	2.	settlement 조회
        var settlement = settlementRepository.findById(hold.getSettlementId())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 settlement 입니다."));
        //	3.	hold 상태 확인
        HoldStatus beforeStatus = hold.getStatus();
        LocalDateTime now = LocalDateTime.now();

        //		5.	RELEASED면
        //	•	noOp=true
        //	•	noOpReason=ALREADY_RELEASED
        //	•	Action.HOLD_RELEASED
        //	•	audit만 저장
        //	•	200 반환
        if (beforeStatus == HoldStatus.RELEASED) {
            String metaJson = buildMetaJson(
                    command.comment(),
                    hold.getRequestedReasonCode().name(),
                    beforeStatus.name(),
                    beforeStatus.name(),
                    true,
                    "ALREADY_RELEASED"
            );

            auditLogger.log(AuditLogCommand.builder()
                    .requestId(command.requestId())
                    .merchantId(settlement.getMerchantId())
                    .entityType(EntityType.HOLD)
                    .entityId(hold.getHoldId())
                    .occurredAt(now)
                    .actorType(ActorType.ADMIN)
                    .actorId(command.actorId())
                    .action(Action.HOLD_RELEASED)
                    .statusBefore(beforeStatus.name())
                    .statusAfter(beforeStatus.name())
                    .metaJson(metaJson)
                    .build());

            return new HoldDecisionResponse(
                    command.requestId(),
                    hold.getStatus(),
                    now
            );

        }

        //		6.	HOLD_REQUESTED면
        //	•	409 HOLD_NOT_ACTIVE
        if (beforeStatus == HoldStatus.HOLD_REQUESTED)
            throw new ConflictException(ReasonCode.HOLD_NOT_ACTIVE,"release 가능한 hold 상태가 아닙니다.");

        //7.	HOLD_ACTIVE면
        //	•	hold.release()
        //	•	settlement.restoreReady()
        //	•	hold_event(HOLD_RELEASED) 저장
        //	•	audit 저장
        //	•	200 반환
        if (beforeStatus == HoldStatus.HOLD_ACTIVE) {
            hold.release();
            settlement.restoreReady();

            String metaJson = buildMetaJson(
                    command.comment(),
                    hold.getRequestedReasonCode().name(),
                    beforeStatus.name(),
                    HoldStatus.RELEASED.name(),
                    false,
                    null
            );

            holdEventRepository.save(HoldEvent.of(
                    hold.getHoldId(),
                    Action.HOLD_RELEASED,
                    command.requestId(),
                    ActorType.ADMIN,
                    command.actorId(),
                    now,
                    metaJson
            ));

            auditLogger.log(AuditLogCommand.builder()
                    .requestId(command.requestId())
                    .merchantId(settlement.getMerchantId())
                    .entityType(EntityType.HOLD)
                    .occurredAt(now)
                    .actorType(ActorType.ADMIN)
                    .actorId(command.actorId())
                    .action(Action.HOLD_RELEASED)
                    .statusBefore(beforeStatus.name())
                    .statusAfter(HoldStatus.RELEASED.name())
                    .entityId(hold.getHoldId())
                    .metaJson(metaJson)
                    .build());


            return new HoldDecisionResponse(
                    command.requestId(),
                    HoldStatus.RELEASED,
                    now
            );
        }

        throw new IllegalStateException("unsupported hold status: " + beforeStatus);
    }

    private String buildMetaJson(
            String comment,
            String reasonCode,
            String before,
            String after,
            boolean noOp,
            String noOpReason
    ) {
        try {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("comment", comment);
            meta.put("reasonCode", reasonCode);
            meta.put("noOp", noOp);

            if (noOp && noOpReason != null && !noOpReason.isBlank()) {
                meta.put("noOpReason", noOpReason);
            }

            Map<String, Object> beforeNode = new LinkedHashMap<>();
            beforeNode.put("status", before);

            Map<String, Object> afterNode = new LinkedHashMap<>();
            afterNode.put("status", after);

            meta.put("before", beforeNode);
            meta.put("after", afterNode);

            return objectMapper.writeValueAsString(meta);
        } catch (Exception e) {
            throw new IllegalStateException("failed to serialize hold audit metaJson", e);
        }
    }
}
