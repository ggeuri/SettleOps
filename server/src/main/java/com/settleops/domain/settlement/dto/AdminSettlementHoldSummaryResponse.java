package com.settleops.domain.settlement.dto;

import java.time.LocalDateTime;

public record AdminSettlementHoldSummaryResponse(
        boolean exists,
        String holdId,
        String status,
        String reasonCode,
        String comment,
        String createdBy,
        LocalDateTime createdAt
) {
    public static AdminSettlementHoldSummaryResponse empty() {
        return new AdminSettlementHoldSummaryResponse(
                false,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
