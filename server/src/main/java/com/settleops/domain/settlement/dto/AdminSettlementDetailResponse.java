package com.settleops.domain.settlement.dto;

import com.settleops.domain.settlement.enums.SettlementStatus;

import java.time.LocalDate;
import java.util.List;

public record AdminSettlementDetailResponse(
        String settlementId,
        String merchantId,
        LocalDate baseDate,
        SettlementStatus status,
        long gross,
        long fee,
        long vat,
        long net,
        List<AdminSettlementLineItemResponse> lines,
        AdminSettlementHoldSummaryResponse hold,
        AdminSettlementRefundSummaryResponse refund
) {
}
