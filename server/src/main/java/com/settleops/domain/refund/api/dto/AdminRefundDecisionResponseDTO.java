package com.settleops.domain.refund.api.dto;

import com.settleops.domain.refund.domain.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminRefundDecisionResponseDTO {
    private RefundStatus status;
    private LocalDateTime decidedAt;
    private String requestId;
}