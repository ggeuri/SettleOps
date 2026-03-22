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
        var settlement = settlementRepository.findById(command.settlementId())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 settlement 입니다."));

        if (settlement.isPaid()) {
            throw new ConflictException(ReasonCode.PAID_ALREADY, "이미 PAID 상태인 settlement 입니다.");
        }

        if (holdRepository.existsBySettlementId(command.settlementId())) {
            throw new ConflictException(ReasonCode.HOLD_ALREADY_EXISTS, "이미 hold가 존재하는 settlement 입니다.");
        }

        var hold = Hold.requested(
                command.settlementId(),
                command.reasonCode(),
                command.comment(),
                command.actorId()
        );

        var savedHold = holdRepository.save(hold);

        String metaJson = buildMetaJson(
                command.comment(),
                command.reasonCode().name(),
                null,
                savedHold.getStatus().name(),
                false,
                null
        );

        holdEventRepository.save(HoldEvent.of(
                savedHold.getHoldId(),
                Action.HOLD_REQUESTED,
                command.requestId(),
                ActorType.ADMIN,
                command.actorId(),
                savedHold.getCreatedAt(),
                metaJson
        ));

        auditLogger.log(AuditLogCommand.builder()
                .requestId(command.requestId())
                .merchantId(settlement.getMerchantId())
                .entityType(EntityType.HOLD)
                .occurredAt(savedHold.getCreatedAt())
                .actorType(ActorType.ADMIN)
                .actorId(command.actorId())
                .action(Action.HOLD_REQUESTED)
                .statusBefore(null)
                .statusAfter(savedHold.getStatus().name())
                .entityId(savedHold.getHoldId())
                .metaJson(metaJson)
                .build());

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
        Hold hold = holdRepository.findById(holdId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 hold 입니다."));

        var settlement = settlementRepository.findById(hold.getSettlementId())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 settlement 입니다."));

        HoldStatus beforeStatus = hold.getStatus();
        LocalDateTime now = LocalDateTime.now();

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

        if (beforeStatus == HoldStatus.HOLD_REQUESTED) {
            // 추가 1: settlement 상태 가드
            if (!settlement.isReady()) {
                throw new ConflictException(
                        ReasonCode.SETTLEMENT_NOT_READY,
                        "READY 상태의 settlement만 Hold approve가 가능합니다."
                );
            }

            String metaJson = buildMetaJson(
                    command.comment(),
                    hold.getRequestedReasonCode().name(),
                    beforeStatus.name(),
                    HoldStatus.HOLD_ACTIVE.name(),
                    false,
                    null
            );

            hold.approve();
            settlement.markHoldActive();

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
        Hold hold = holdRepository.findById(holdId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 hold 입니다."));

        var settlement = settlementRepository.findById(hold.getSettlementId())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 settlement 입니다."));

        HoldStatus beforeStatus = hold.getStatus();
        LocalDateTime now = LocalDateTime.now();

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

        if (beforeStatus == HoldStatus.HOLD_REQUESTED) {
            throw new ConflictException(
                    ReasonCode.HOLD_NOT_ACTIVE,
                    "release 가능한 hold 상태가 아닙니다."
            );
        }

        if (beforeStatus == HoldStatus.HOLD_ACTIVE) {
            // 추가 2: settlement 상태 가드
            if (!settlement.isHoldActive()) {
                throw new ConflictException(
                        ReasonCode.HOLD_NOT_ACTIVE,
                        "연동 settlement가 HOLD_ACTIVE 상태가 아닙니다."
                );
            }

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