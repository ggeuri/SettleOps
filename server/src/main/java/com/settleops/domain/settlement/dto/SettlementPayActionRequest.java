package com.settleops.domain.settlement.dto;

public record SettlementPayActionRequest(
        String comment // LOCKED: 선택(optional)
) {}