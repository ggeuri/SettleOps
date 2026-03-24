package com.settleops.domain.refund.api.dto;

import com.querydsl.core.annotations.QueryProjection;
import com.settleops.domain.refund.domain.RefundStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class RefundRowDTO {

    private final String refundId;
    private final String paymentId;
    private final String merchantId;
    private final long amount;
    private final RefundStatus status;
    private final String reasonText;
    private final LocalDateTime requestedAt;
    private final LocalDateTime decidedAt;

    @QueryProjection
    public RefundRowDTO(
            String refundId,
            String paymentId,
            String merchantId,
            long amount,
            RefundStatus status,
            String reasonText,
            LocalDateTime requestedAt,
            LocalDateTime decidedAt
    ) {
        this.refundId = refundId;
        this.paymentId = paymentId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.status = status;
        this.reasonText = reasonText;
        this.requestedAt = requestedAt;
        this.decidedAt = decidedAt;
    }
}