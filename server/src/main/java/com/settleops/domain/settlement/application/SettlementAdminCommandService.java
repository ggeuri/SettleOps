package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.dto.SettlementBatchRunResponse;
import com.settleops.domain.settlement.dto.SettlementPayActionResponse;

import java.time.LocalDate;

public interface SettlementAdminCommandService {
    /**
     * A2 배치 실행(운영 리런)
     * LOCKED:
     * - baseDate 동일이면 SKIP + 기존 run_id/result 반환
     * - FAIL 기록은 반드시 남아야 함(실패로 롤백되면 안 됨)
     */
    SettlementBatchRunResponse runBatch(LocalDate baseDate, String requestId, String adminId);

    /**
     * A4 4-eyes 1단계: request-paid
     * LOCKED 409 reason:
     * - SETTLEMENT_NOT_READY
     * - HOLD_ACTIVE
     * - BATCH_FAILED
     * - REFUND_ADJUSTMENT_PENDING
     */
    SettlementPayActionResponse requestPaid(String settlementId, String comment, String requestId, String adminId);

    /**
     * A4 4-eyes 2단계: approve-paid
     * LOCKED:
     * - 이미 PAID면 no-op 200 + (status=PAID + paidAt) 필수
     * - PAY_REQUESTED가 아니면 409 PAY_REQUESTED_REQUIRED
     * - SAME_APPROVER_NOT_ALLOWED
     * - (옵션) IN_PROGRESS
     */
    SettlementPayActionResponse approvePaid(String settlementId, String comment, String requestId, String adminId);
}