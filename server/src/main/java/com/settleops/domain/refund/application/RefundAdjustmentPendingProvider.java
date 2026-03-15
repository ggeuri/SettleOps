package com.settleops.domain.refund.application;

/**
 * REFUND_ADJUSTMENT_PENDING 판정용 Provider.
 *
 * 책임:
 * - 특정 settlementId에 대해
 *   "APPROVED refund가 있고, 아직 refund_settlement_link 증거가 없는지"를 제공
 *
 * 비책임:
 * - settlement 상태 변경
 * - request-paid 차단 처리
 */
public interface RefundAdjustmentPendingProvider {

    boolean existsPendingForSettlement(String settlementId);
}