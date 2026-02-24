package com.settleops.domain.settlement.application;

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
import com.settleops.global.error.ConflictException;
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

@Service
@RequiredArgsConstructor
public class SettlementAdminCommandServiceImpl implements SettlementAdminCommandService {

    private final SettlementRepository settlementRepository;
    private final SettlementBatchRepository settlementBatchRepository;
    private final AuditLogger auditLogger;

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
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "settlement not found"));

        // 1) no-op 200: 이미 PAY_REQUESTED면 현재 상태 반환 (+ audit)
        if (settlement.isPayRequested()) {
            auditSettlementAction(
                    Action.SETTLEMENT_PAY_REQUESTED,
                    settlement,
                    settlement.getStatus().name(),
                    settlement.getStatus().name(),
                    comment
            );
            return toPayActionResponse(settlement);
        }

        // 2) 409 reason 우선순위 고정(LOCKED)
        // 2-1) SETTLEMENT_NOT_READY (단, HOLD_ACTIVE는 별도 reason으로 내보내기 위해 제외)
        if (!settlement.isReady() && !settlement.isHoldActive()) {
            throw new ConflictException(ReasonCode.SETTLEMENT_NOT_READY, "settlement is not READY");
        }

        // 2-2) HOLD_ACTIVE
        if (settlement.isHoldActive()) {
            throw new ConflictException(ReasonCode.HOLD_ACTIVE, "hold is active");
        }

        // 2-3) BATCH_FAILED
        if (isBatchFailed(settlement)) {
            throw new ConflictException(ReasonCode.BATCH_FAILED, "batch failed");
        }

        // 2-4) REFUND_ADJUSTMENT_PENDING 은 C 의존 → 다음 PR에서 추가
        // if (refundAdjustmentPending(...)) throw new ConflictException(ReasonCode.REFUND_ADJUSTMENT_PENDING, "...");

        // 3) 상태 전이 + 저장
        String before = settlement.getStatus().name();
        String requesterId = currentActorId();

        settlement.requestPaid(requesterId, LocalDateTime.now());
        settlementRepository.save(settlement);

        // 4) audit (LOCKED): comment는 meta_json.comment에 반드시 기록
        auditSettlementAction(
                Action.SETTLEMENT_PAY_REQUESTED,
                settlement,
                before,
                settlement.getStatus().name(),
                comment
        );

        return toPayActionResponse(settlement);
    }

    @Override
    @Transactional
    public SettlementPayActionResponse approvePaid(String settlementId, String comment) {
        Settlement current = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "settlement not found"));

        // 1) no-op 200: 이미 PAID (+ audit)
        if (current.isPaid()) {
            auditSettlementAction(
                    Action.SETTLEMENT_PAY_APPROVED,
                    current,
                    current.getStatus().name(),
                    current.getStatus().name(),
                    comment
            );
            return toPayActionResponse(current);
        }

        // 2) PAY_REQUESTED 아니면 409
        if (!current.isPayRequested()) {
            throw new ConflictException(ReasonCode.PAY_REQUESTED_REQUIRED, "PAY_REQUESTED status required");
        }

        // 3) PAY_REQUESTED일 때만 락 조회
        Settlement settlement = settlementRepository.findByIdForUpdate(settlementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "settlement not found"));

        // 락 후 재확인(no-op) (+ audit)
        if (settlement.isPaid()) {
            auditSettlementAction(
                    Action.SETTLEMENT_PAY_APPROVED,
                    settlement,
                    settlement.getStatus().name(),
                    settlement.getStatus().name(),
                    comment
            );
            return toPayActionResponse(settlement);
        }

        String approverId = currentActorId();

        if (settlement.violatesFourEyes(approverId)) {
            throw new ConflictException(ReasonCode.SAME_APPROVER_NOT_ALLOWED, "requester and approver must be different");
        }

        String before = settlement.getStatus().name();
        settlement.approvePaid(approverId, LocalDateTime.now());
        settlementRepository.save(settlement);

        auditSettlementAction(
                Action.SETTLEMENT_PAY_APPROVED,
                settlement,
                before,
                settlement.getStatus().name(),
                comment
        );

        return toPayActionResponse(settlement);
    }

    private boolean isBatchFailed(Settlement settlement) {
        Long batchId = settlement.getBatchId();
        if (batchId == null) return false; // 방어 (DDL상 nullable=false지만 안전하게 유지)

        return settlementBatchRepository.findById(batchId)
                .map(SettlementBatch::getResult)
                .map(r -> r == SettlementBatchResult.FAIL)
                .orElse(false);
    }

    private SettlementPayActionResponse toPayActionResponse(Settlement s) {
        LocalDateTime paidAt = s.isPaid() ? s.getPaidApprovedAt() : null;
        if (s.isPaid() && paidAt == null) {
            throw new IllegalStateException("paidAt must not be null when status is PAID");
        }

        return new SettlementPayActionResponse(
                currentRequestId(),
                s.getSettlementId(),
                s.getStatus(),
                s.getPaidRequestedAt(),
                paidAt
        );
    }

    private String currentActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new IllegalStateException("actorId must not be null/blank");
        }
        return auth.getName();
    }

    private String currentRequestId() {
        String requestId = MDC.get("requestId");
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalStateException("requestId must not be null/blank (provided by RequestIdFilter)");
        }
        return requestId;
    }

    private void auditSettlementAction(Action action, Settlement settlement, String before, String after, String comment) {
        AuditLogCommand cmd = AuditLogCommand.builder()
                .requestId(currentRequestId())
                .merchantId(settlement.getMerchantId())
                .entityType(EntityType.SETTLEMENT)
                .entityId(settlement.getSettlementId())
                .occurredAt(LocalDateTime.now())
                .actorType(ActorType.ADMIN)
                .actorId(currentActorId())
                .action(action)
                .statusBefore(before)
                .statusAfter(after)
                .metaJson(comment == null ? "{}" : "{\"comment\":" + toJsonString(comment) + "}")
                .build();

        auditLogger.log(cmd);
    }

    private String toJsonString(String s) {
        if (s == null) return "null";
        String escaped = s.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }
}