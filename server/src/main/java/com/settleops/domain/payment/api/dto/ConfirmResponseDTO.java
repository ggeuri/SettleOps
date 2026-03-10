package com.settleops.domain.payment.api.dto;

import com.settleops.domain.payment.domain.Payment;

import java.time.LocalDateTime;

public record ConfirmResponseDTO(
        String paymentId,
        String orderId,
        String status,
        LocalDateTime confirmedAt
) {
    public static ConfirmResponseDTO from(Payment payment, LocalDateTime confirmedAt) {
        if (payment == null) {
            throw new IllegalArgumentException("payment is null");
        }
        if (confirmedAt == null) {
            throw new IllegalArgumentException("confirmedAt is null");
        }

        return new ConfirmResponseDTO(
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getStatus().name(),
                confirmedAt // SoT: PAYMENT_CONFIRMED.occurred_at
        );
    }
}