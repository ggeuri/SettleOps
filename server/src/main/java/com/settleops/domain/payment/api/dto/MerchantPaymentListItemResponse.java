package com.settleops.domain.payment.api.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MerchantPaymentListItemResponse {

    private final String paymentId;
    private final String orderId;
    private final String status;
    private final long requestedAmount;
    private final long capturedAmount;
    private final String currency;
    private final String buyerId;
    private final LocalDateTime capturedAt;
    private final boolean confirmed;
    private final LocalDateTime confirmedAt;

    public MerchantPaymentListItemResponse(
            String paymentId,
            String orderId,
            String status,
            long requestedAmount,
            long capturedAmount,
            String currency,
            String buyerId,
            LocalDateTime capturedAt,
            boolean confirmed,
            LocalDateTime confirmedAt
    ) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.status = status;
        this.requestedAmount = requestedAmount;
        this.capturedAmount = capturedAmount;
        this.currency = currency;
        this.buyerId = buyerId;
        this.capturedAt = capturedAt;
        this.confirmed = confirmed;
        this.confirmedAt = confirmedAt;
    }
}