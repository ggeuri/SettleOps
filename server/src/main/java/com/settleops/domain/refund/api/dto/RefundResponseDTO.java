package com.settleops.domain.refund.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RefundResponseDTO {
    private String refundId;
    private String paymentId;
    private long amount;
    private String status;
    private LocalDateTime requestedAt;

}
