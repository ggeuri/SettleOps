package com.settleops.domain.order.infra;

import java.time.LocalDateTime;

public record ConsumerOrderListFlatRow(
        String orderId,
        String buyerId,
        String itemName,
        Long amount,
        String orderStatus,
        String paymentId,
        LocalDateTime confirmedAt,
        LocalDateTime createdAt
) {
}