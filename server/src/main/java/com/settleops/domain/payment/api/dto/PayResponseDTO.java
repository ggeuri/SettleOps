package com.settleops.domain.payment.api.dto;

import com.settleops.domain.payment.domain.Payment;

import java.time.LocalDateTime;


public record PayResponseDTO(
        String paymentId,
        String status,
        LocalDateTime capturedAt
) {
    public static PayResponseDTO from(Payment payment, LocalDateTime capturedAt) {
        return new PayResponseDTO(
                payment.getPaymentId(),
                payment.getStatus().name(), // status = PaymentStatus enum
                capturedAt // 추후 변경 예정 PAYMENT_CAPTURED event의 occurred_at을 조회
        );
    }
}
