package com.settleops.domain.order.infra;

import com.settleops.domain.order.domain.OrderStatus;

import java.time.LocalDateTime;

/**
 * Consumer C3 목록 조회용 flat row
 */
public record ConsumerOrderListFlatRow(
        String orderId,
        String buyerId,
        String itemName,
        Long amount,
        OrderStatus orderStatus,
        String paymentId,
        LocalDateTime confirmedAt,
        LocalDateTime createdAt
) {
}