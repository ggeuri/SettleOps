package com.settleops.domain.settlement.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.settleops.domain.settlement.dto.SettlementBatchRunResponse;
import com.settleops.domain.settlement.dto.SettlementPayActionResponse;
import com.settleops.domain.settlement.entity.Settlement;
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
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SettlementAdminCommandServiceImpl implements SettlementAdminCommandService {

    private final SettlementRepository settlementRepository;
    private final SettlementBatchRepository settlementBatchRepository;
    private final AuditLogger auditLogger;
    private final RefundAdjustmentPolicy refundAdjustmentPolicy;
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
        if (settlementId == null || settlementId.isBlank()) {
            throw new IllegalStateException("settlementId must not be null/blank");
        }

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

        // 2) 409 reason 우선순위 고정(LOCKED, 기획서 SoT)
        //    SETTLEMENT_NOT_READY → HOLD_ACTIVE → BATCH_FAILED → REFUND_ADJUSTMENT_PENDING

        // 2-1) SETTLEMENT_NOT_READY (READY 아니면 무조건 최우선)
        if (!settlement.isReady()) {
            throw new ConflictException(ReasonCode.SETTLEMENT_NOT_READY, "settlement is not READY");
        }

        // 2-2) HOLD_ACTIVE
        if (settlement.isHoldActive()) {
            throw new ConflictException(ReasonCode.HOLD_ACTIVE, "hold is active");
        }

        // 2-3) BATCH_FAILED
        if (isBatchFailed(settlement.getBatchId())) {
            throw new ConflictException(ReasonCode.BATCH_FAILED, "batch failed");
        }

        // 2-4) REFUND_ADJUSTMENT_PENDING
        if (refundAdjustmentPolicy.isRefundAdjustmentPending(settlementId)) {
            throw new ConflictException(ReasonCode.REFUND_ADJUSTMENT_PENDING, "refund adjustment pending");
        }

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
        if (settlementId == null || settlementId.isBlank()) {
            throw new IllegalStateException("settlementId must not be null/blank");
        }

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

    /**
     * BATCH_FAILED 판정 (가드레일)
     * - batchId null 방어 포함.
     */
    private boolean isBatchFailed(Long batchId) {
        if (batchId == null) return false;
        return settlementBatchRepository.existsByBatchIdAndResult(batchId, SettlementBatchResult.FAIL);
    }

    private SettlementPayActionResponse toPayActionResponse(Settlement s) {
        // PAY_REQUESTED(no-op 200 포함) 규격: paidRequestedAt 필수
        if (s.isPayRequested() && s.getPaidRequestedAt() == null) {
            throw new IllegalStateException("paidRequestedAt must not be null when status is PAY_REQUESTED");
        }

        // PAID(no-op 200 포함) 규격: paidApprovedAt 필수
        if (s.isPaid() && s.getPaidApprovedAt() == null) {
            throw new IllegalStateException("paidAt must not be null when status is PAID");
        }

        LocalDateTime paidAt = s.isPaid() ? s.getPaidApprovedAt() : null;

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
                // 안전 JSON + comment 규칙 단일화: 항상 {"comment": ...} (없으면 null)
                .metaJson(buildMetaJson(comment))
                .build();

        auditLogger.log(cmd);
    }

    /**
     * audit_log.meta_json 생성(LOCKED)
     * - 문자열 조립 금지: 제어문자/개행/따옴표로 JSON 파손 방지
     * - comment 키는 항상 포함(없으면 null)하여 MemoThreading/필터 품질을 고정한다.
     */
    private String buildMetaJson(String comment) {
        try {
            Map<String, Object> meta = new HashMap<>();
            meta.put("comment", comment); // null이면 JSON null
            return objectMapper.writeValueAsString(meta);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize audit metaJson", e);
        }
    }
}