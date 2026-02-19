package com.settleops.domain.settlement.dto;

import com.settleops.domain.settlement.enums.SettlementStatus;

import java.time.LocalDate;

public record SettlementSummaryResponse(
        String settlementId,
        String settlementNo,
        Long batchId,
        String merchantId,
        LocalDate baseDate,
        SettlementStatus status,
        long gross,
        long fee,
        long vat,
        long net
) {}