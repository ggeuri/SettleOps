package com.settleops.domain.order.infra;

import com.settleops.domain.order.domain.OrderStatus;

import java.time.LocalDateTime;

public record ConsumerOrderFlatRow(
        String orderId,
        String merchantId,
        String buyerId,
        String itemName,
        Long amount,
        String currency,
        OrderStatus orderStatus,
        String paymentId,
        String paymentStatus,
        LocalDateTime capturedAt,
        LocalDateTime confirmedAt
) {
}