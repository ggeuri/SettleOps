package com.settleops.domain.order.api.dto;

import com.settleops.domain.order.domain.OrderStatus;

import java.time.LocalDateTime;

/**
 * Consumer C3 주문/결제 목록 row
 *
 * - orderStatus: orders.status SoT
 * - paid: orderStatus 기반 파생값 (PAID면 true)
 * - confirmed: PAYMENT_CONFIRMED 이벤트 존재 여부 기반 파생값
 */
public record ConsumerOrderListItemResponse(
        String orderId,
        String itemName,
        long amount,
        OrderStatus orderStatus,
        boolean paid,
        boolean confirmed,
        LocalDateTime createdAt
) {
}