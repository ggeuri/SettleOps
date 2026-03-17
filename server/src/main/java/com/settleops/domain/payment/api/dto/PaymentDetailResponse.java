package com.settleops.domain.payment.api.dto;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class PaymentDetailResponse {

    private String paymentId;
    private String orderId;
    private String merchantId;
    private String buyerId;
    private String status;
    private long requestedAmount;
    private long capturedAmount;
    private String currency;
    private LocalDateTime createdAt;
    private LocalDateTime capturedAt;
    private boolean confirmed;
    private LocalDateTime confirmedAt;
    private List<PaymentEventItem> events = List.of();

    public PaymentDetailResponse(
            String paymentId,
            String orderId,
            String merchantId,
            String buyerId,
            String status,
            long requestedAmount,
            long capturedAmount,
            String currency,
            LocalDateTime createdAt,
            LocalDateTime capturedAt,
            boolean confirmed,
            LocalDateTime confirmedAt
    ) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.merchantId = merchantId;
        this.buyerId = buyerId;
        this.status = status;
        this.requestedAmount = requestedAmount;
        this.capturedAmount = capturedAmount;
        this.currency = currency;
        this.createdAt = createdAt;
        this.capturedAt = capturedAt;
        this.confirmed = confirmed;
        this.confirmedAt = confirmedAt;
    }

    public void assignEvents(List<PaymentEventItem> events) {
        this.events = (events == null) ? List.of() : events;
    }

    @Getter
    public static class PaymentEventItem {
        private String eventType;
        private String statusBefore;
        private String statusAfter;
        private LocalDateTime occurredAt;

        public PaymentEventItem(
                String eventType,
                String statusBefore,
                String statusAfter,
                LocalDateTime occurredAt
        ) {
            this.eventType = eventType;
            this.statusBefore = statusBefore;
            this.statusAfter = statusAfter;
            this.occurredAt = occurredAt;
        }
    }
}