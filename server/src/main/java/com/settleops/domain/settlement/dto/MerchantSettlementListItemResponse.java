package com.settleops.domain.settlement.dto;

import com.settleops.domain.settlement.enums.SettlementStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MerchantSettlementListItemResponse (
        String settlementId,
        LocalDate baseDate,
        SettlementStatus status,
        long gross,
        long fee,
        long vat,
        long net,
        LocalDateTime createdAt
){
}
