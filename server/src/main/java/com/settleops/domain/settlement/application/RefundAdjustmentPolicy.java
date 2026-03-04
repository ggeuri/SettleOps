package com.settleops.domain.settlement.application;

/**
 * Refund adjustment pending 판정 정책(LOCKED)
 *
 * - 지급 요청(request-paid) 가드레일에서 사용
 * - 판정 SoT는 refund_settlement_link 단일(조인 추론 금지)
 * - 구현체는 Refund 도메인(C 오너)에서 제공
 */
public interface RefundAdjustmentPolicy {
    boolean isRefundAdjustmentPending(String settlementId);
}