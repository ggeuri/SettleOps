package com.settleops.domain.settlement.dto;

import com.settleops.domain.settlement.enums.SettlementLineType;

public record AdminSettlementLineItemResponse (
        String settlementLineId,
        SettlementLineType type,
        String paymentId,
        long amount
){
}
