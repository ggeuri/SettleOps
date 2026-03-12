package com.settleops.domain.refund.application.dto;

import java.time.LocalDateTime;

/**
 * C 오너가 A 배치에 넘겨주는 "REFUND line 입력 집합"의 1건.
 *
 * LOCKED:
 * - approved refund 중
 * - 아직 refund_settlement_link가 없는 것만
 * - 다음 배치에서 REFUND line으로 반영할 입력
 */
public record ApprovedRefundAdjustment(
        String refundId,
        String paymentId,
        String merchantId,
        long amount,
        LocalDateTime decidedAt
) {
}