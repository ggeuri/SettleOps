package com.settleops.domain.settlement.dto;

public record AdminSettlementRefundSummaryResponse (
        boolean hasApprovedRefund,
        boolean refundAdjustmentPending
){
    public static AdminSettlementRefundSummaryResponse empty() {
        return new AdminSettlementRefundSummaryResponse(false, false);
    }
}
