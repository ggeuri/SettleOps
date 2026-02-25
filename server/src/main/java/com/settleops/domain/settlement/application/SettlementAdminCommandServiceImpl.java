package com.settleops.domain.settlement.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.settleops.domain.settlement.dto.SettlementBatchRunResponse;
import com.settleops.domain.settlement.dto.SettlementPayActionResponse;
import com.settleops.domain.settlement.entity.Settlement;
import com.settleops.domain.settlement.entity.SettlementBatch;
import com.settleops.domain.settlement.enums.SettlementBatchResult;
import com.settleops.domain.settlement.infra.SettlementBatchRepository;
import com.settleops.domain.settlement.infra.SettlementRepository;
import com.settleops.global.audit.ActorType;
import com.settleops.global.audit.AuditLogCommand;
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.audit.EntityType;
import com.settleops.global.enums.Action;
import com.settleops.global.enums.ReasonCode;
import com.settleops.global.error.BadRequestException;
import com.settleops.global.error.ConflictException;
import com.settleops.global.logging.RequestIdKeys;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SettlementAdminCommandServiceImpl implements SettlementAdminCommandService {

    private final SettlementRepository settlementRepository;
    private final SettlementBatchRepository settlementBatchRepository;
    private final AuditLogger auditLogger;
    private final RefundAdjustmentPolicy refundAdjustmentPolicy;
    private final SettlementBatchRunRecorder settlementBatchRunRecorder;
    private final ObjectMapper objectMapper;

    @Override
    public SettlementBatchRunResponse runBatch(LocalDate baseDate) {
        throw new ResponseStatusException(
                HttpStatus.NOT_IMPLEMENTED,
                "batch run (A2) will be implemented in the next PR."
        );
    }

    @Override
    @Transactional
    public SettlementPayActionResponse requestPaid(String settlementId, String comment) {
        // LOCKED: requestId/actorId 스냅샷 고정(요청 단위 재현 SoT)
        String requestId = currentRequestId();
        String actorId = currentActorId();

        if (settlementId == null || settlementId.isBlank()) {
            throw new BadRequestException("settlementId must not be null/blank");
        }

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "settlement not found"));

        // 1) no-op 200(LOCKED): 이미 PAY_REQUESTED면 상태 유지 + audit만 기록
        if (settlement.isPayRequested()) {
            String before = settlement.getStatus().name();
            String after = settlement.getStatus().name();

            auditSettlementAction(
                    requestId,
                    actorId,
                    Action.SETTLEMENT_PAY_REQUESTED,
                    settlement,
                    before,
                    after,
                    comment
            );
            return toPayActionResponse(settlement, requestId);
        }

        // 2) 409 reason 우선순위(LOCKED)
        //    HOLD_ACTIVE → SETTLEMENT_NOT_READY → BATCH_FAILED → REFUND_ADJUSTMENT_PENDING
        //    (HOLD_ACTIVE는 별도 reason으로 반환되어야 하므로 최우선으로 분리)
        if (settlement.isHoldActive()) {
            throw new ConflictException(ReasonCode.HOLD_ACTIVE, "hold is active");
        }

        if (!settlement.isReady()) {
            throw new ConflictException(ReasonCode.SETTLEMENT_NOT_READY, "settlement is not READY");
        }

        if (isBatchFailed(settlement.getBaseDate())) {
            throw new ConflictException(ReasonCode.BATCH_FAILED, "batch failed");
        }

        if (refundAdjustmentPolicy.isRefundAdjustmentPending(settlementId)) {
            throw new ConflictException(ReasonCode.REFUND_ADJUSTMENT_PENDING, "refund adjustment pending");
        }

        // 3) 상태 전이(SoT): READY -> PAY_REQUESTED
        String before = settlement.getStatus().name();
        settlement.requestPaid(actorId, LocalDateTime.now());
        String after = settlement.getStatus().name();

        // 4) audit (LOCKED): requestId/actorId는 스냅샷 값 사용
        auditSettlementAction(
                requestId,
                actorId,
                Action.SETTLEMENT_PAY_REQUESTED,
                settlement,
                before,
                after,
                comment
        );

        return toPayActionResponse(settlement, requestId);
    }

    @Override
    @Transactional
    public SettlementPayActionResponse approvePaid(String settlementId, String comment) {
        // LOCKED: requestId/actorId 스냅샷 고정
        String requestId = currentRequestId();
        String approverId = currentActorId();

        if (settlementId == null || settlementId.isBlank()) {
            throw new BadRequestException("settlementId must not be null/blank");
        }

        // 1) 락 없이 조회: 없으면 404
        Settlement current = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "settlement not found"));

        // 2) no-op 200(LOCKED): 이미 PAID면 audit + 현재 상태 반환
        if (current.isPaid()) {
            String before = current.getStatus().name();
            String after = current.getStatus().name();

            auditSettlementAction(
                    requestId,
                    approverId,
                    Action.SETTLEMENT_PAY_APPROVED,
                    current,
                    before,
                    after,
                    comment
            );
            return toPayActionResponse(current, requestId);
        }

        // 3) PAY_REQUESTED 아니면 409
        if (!current.isPayRequested()) {
            throw new ConflictException(ReasonCode.PAY_REQUESTED_REQUIRED, "PAY_REQUESTED status required");
        }

        // 4) PAY_REQUESTED일 때만 락 조회(PESSIMISTIC_WRITE)
        Settlement settlement = settlementRepository.findByIdForUpdate(settlementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "settlement not found"));

        // 락 후 재확인(no-op)
        if (settlement.isPaid()) {
            String before = settlement.getStatus().name();
            String after = settlement.getStatus().name();

            auditSettlementAction(
                    requestId,
                    approverId,
                    Action.SETTLEMENT_PAY_APPROVED,
                    settlement,
                    before,
                    after,
                    comment
            );
            return toPayActionResponse(settlement, requestId);
        }

        // 4-eyes 위반 차단(LOCKED)
        if (settlement.violatesFourEyes(approverId)) {
            throw new ConflictException(
                    ReasonCode.SAME_APPROVER_NOT_ALLOWED,
                    "requester and approver must be different"
            );
        }

        // 5) 상태 전이(SoT): PAY_REQUESTED -> PAID
        String before = settlement.getStatus().name();
        settlement.approvePaid(approverId, LocalDateTime.now());
        String after = settlement.getStatus().name();

        auditSettlementAction(
                requestId,
                approverId,
                Action.SETTLEMENT_PAY_APPROVED,
                settlement,
                before,
                after,
                comment
        );

        return toPayActionResponse(settlement, requestId);
    }

    private boolean isBatchFailed(LocalDate baseDate) {
        if (baseDate == null) return false;
        return settlementBatchRepository.findByBatchKey(baseDate)
                .filter(b -> b.getFinishedAt() != null)
                .map(b -> b.getResult() == SettlementBatchResult.FAIL)
                .orElse(false);
    }

    private SettlementPayActionResponse toPayActionResponse(Settlement s, String requestId) {
        if (s.isPayRequested() && s.getPaidRequestedAt() == null) {
            throw new IllegalStateException("paidRequestedAt must not be null when status is PAY_REQUESTED");
        }
        if (s.isPaid() && s.getPaidApprovedAt() == null) {
            throw new IllegalStateException("paidAt must not be null when status is PAID");
        }

        LocalDateTime paidAt = s.isPaid() ? s.getPaidApprovedAt() : null;

        return new SettlementPayActionResponse(
                requestId,
                s.getSettlementId(),
                s.getStatus(),
                s.getPaidRequestedAt(),
                paidAt
        );
    }

    // request_id 생성/주입은 Filter 단일 책임(Freeze). 여기서는 읽기만 한다.
    private String currentRequestId() {
        String requestId = MDC.get(RequestIdKeys.MDC_KEY);
        if (requestId == null || requestId.isBlank()) {
            throw new BadRequestException("requestId must not be null/blank (RequestIdFilter contract violated)");
        }
        return requestId;
    }

    // 인증 누락은 401로 귀결(500 금지)
    private String currentActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized (authentication required)");
        }
        return auth.getName();
    }

    private void auditSettlementAction(
            String requestId,
            String actorId,
            Action action,
            Settlement settlement,
            String before,
            String after,
            String comment
    ) {
        AuditLogCommand cmd = AuditLogCommand.builder()
                .requestId(requestId)
                .merchantId(settlement.getMerchantId())
                .entityType(EntityType.SETTLEMENT)
                .entityId(settlement.getSettlementId())
                .occurredAt(LocalDateTime.now())
                .actorType(ActorType.ADMIN)
                .actorId(actorId)
                .action(action)
                .statusBefore(before)
                .statusAfter(after)
                .metaJson(buildMetaJson(comment))
                .build();

        auditLogger.log(cmd);
    }

    private String buildMetaJson(String comment) {
        try {
            Map<String, Object> meta = new HashMap<>();
            if (comment != null && !comment.isBlank()) {
                meta.put("comment", comment);
            }
            return objectMapper.writeValueAsString(meta);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize audit metaJson", e);
        }
    }

    @SuppressWarnings("unused")
    private SettlementBatchRunResponse toBatchRunResponse(
            String requestId,
            SettlementBatch batch,
            SettlementBatchRunResponse.RunResult result
    ) {
        String failReason = (result == SettlementBatchRunResponse.RunResult.FAIL) ? batch.getFailReason() : null;

        return new SettlementBatchRunResponse(
                requestId,
                batch.getBatchKey(),
                batch.getRunId(),
                result,
                failReason,
                batch.getCreatedAt(),
                batch.getFinishedAt()
        );
    }
}