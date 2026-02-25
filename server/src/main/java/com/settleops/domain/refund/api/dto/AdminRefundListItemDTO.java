package com.settleops.domain.refund.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminRefundListItemDTO {
    private String refundId;
    private String paymentId;
    private String merchantId;
    private long amount;
    private String status;
    private LocalDateTime requestedAt;
    private LocalDateTime decidedAt;
}
