package com.settleops.domain.refund.api.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public class RefundResponseDTO {
    String refundId;
    int paymentId;
    int amount;
    String status;
    LocalDateTime requestedAt;

}
