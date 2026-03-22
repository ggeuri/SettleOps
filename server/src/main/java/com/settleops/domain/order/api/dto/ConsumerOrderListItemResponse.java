package com.settleops.domain.order.api.dto;

import java.time.LocalDateTime;

public record ConsumerOrderListItemResponse(
        String orderId,
        String itemName,
        long amount,
        String orderStatus,
        boolean paid,
        boolean confirmed,
        LocalDateTime createdAt
) {
}