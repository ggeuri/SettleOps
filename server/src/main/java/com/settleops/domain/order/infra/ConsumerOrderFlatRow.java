package com.settleops.domain.order.infra;

import java.time.LocalDateTime;

public record ConsumerOrderFlatRow(
        String orderId,
        String merchantId,
        String buyerId,
        String itemName,
        Long amount,
        String orderStatus,
        String paymentId,
        String paymentStatus,
        LocalDateTime capturedAt,
        LocalDateTime confirmedAt
) {
}