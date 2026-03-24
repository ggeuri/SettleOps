package com.settleops.domain.refund.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RefundTraceEntryResponseDTO {
    private final String traceRequestId;
}