package com.settleops.domain.settlement.dto;

import com.settleops.domain.settlement.enums.SettlementStatus;

import java.time.LocalDateTime;

public record SettlementPayActionResponse(
        String requestId,
        String settlementId,
        SettlementStatus status,
        LocalDateTime paidRequestedAt,
        LocalDateTime paidAt // (= approved 시각). 이미 PAID면 no-op 200에서도 이 값 필수
) {}