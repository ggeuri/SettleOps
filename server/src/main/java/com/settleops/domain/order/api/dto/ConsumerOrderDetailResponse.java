package com.settleops.domain.order.api.dto;

import com.settleops.domain.order.domain.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Consumer 주문 상세 조회 응답
 *
 * - orderStatus: orders.status SoT
 * - paymentStatus: payment.status 문자열 표현
 * - confirmedAt: PAYMENT_CONFIRMED 이벤트 occurredAt 파생값
 */
public record ConsumerOrderDetailResponse(
        String orderId,
        String merchantId,
        String buyerId,
        String itemName,
        long amount,
        String currency,
        OrderStatus orderStatus,
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