package com.settleops.domain.refund.api.dto;

import com.settleops.domain.refund.domain.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminRefundDecisionResponseDTO {

    // no-op 포함 최소필드
    private RefundStatus status;
    private LocalDateTime decidedAt;
    private String requestId;
}