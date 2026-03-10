package com.settleops.domain.settlement.dto;

import com.settleops.domain.settlement.enums.SettlementStatus;

import java.time.LocalDate;

public record AdminSettlementListItemResponse(
        String settlementId,
        String merchantId,
        SettlementStatus status,
        LocalDate baseDate,
        long net
){ }
