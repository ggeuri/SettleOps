package com.settleops.domain.order.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ConsumerOrderDetailResponse(
        String orderId,
        String merchantId,
        String buyerId,
        String itemName,
        long amount,
        String orderStatus,
        String paymentId,
        String paymentStatus,
        LocalDateTime capturedAt,
        LocalDateTime confirmedAt,
        List<PaymentEventItem> events
) {
    public record PaymentEventItem(
            String eventType,
            String statusBefore,
            String statusAfter,
            LocalDateTime occurredAt
    ) {
    }
}