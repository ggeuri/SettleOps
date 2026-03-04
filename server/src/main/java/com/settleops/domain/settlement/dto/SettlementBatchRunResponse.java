package com.settleops.domain.settlement.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SettlementBatchRunResponse(
        String requestId,
        LocalDate batchKey,
        String runId,
        RunResult result, // 응답용 (OK/FAIL/SKIP)
        String failReason, // OK면 null, FAIL이면 값(없으면 UNEXPECTED_ERROR로 저장됨)
        LocalDateTime createdAt,
        LocalDateTime finishedAt
) {
    public enum RunResult { OK, FAIL, SKIP }
}