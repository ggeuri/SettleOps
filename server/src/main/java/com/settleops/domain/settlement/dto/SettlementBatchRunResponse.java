package com.settleops.domain.settlement.dto;

import com.settleops.domain.settlement.enums.SettlementBatchResult;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SettlementBatchRunResponse(
        String requestId,
        LocalDate batchKey,
        String runId,
        SettlementBatchResult result,
        String failReason, // OK면 null, FAIL이면 값(없으면 UNEXPECTED_ERROR로 저장됨)
        LocalDateTime createdAt,
        LocalDateTime finishedAt
) {}