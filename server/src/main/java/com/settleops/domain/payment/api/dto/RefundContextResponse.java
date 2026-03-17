package com.settleops.domain.payment.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RefundContextResponse {

    private final String paymentId;
    private final String status;
    private final long capturedAmount;
    private final long refundableAmount;
    private final String currency;
    private final String merchantId;
    private final LocalDateTime capturedAt;

    public RefundContextResponse(
            String paymentId,
            String status,
            long capturedAmount,
            long refundableAmount,
            String currency,
            String merchantId,
            LocalDateTime capturedAt
    ) {
        this.paymentId = paymentId;
        this.status = status;
        this.capturedAmount = capturedAmount;
        this.refundableAmount = refundableAmount;
        this.currency = currency;
        this.merchantId = merchantId;
        this.capturedAt = capturedAt;
    }
}