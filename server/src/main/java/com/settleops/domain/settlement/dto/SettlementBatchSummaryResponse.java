package com.settleops.domain.settlement.dto;

import com.settleops.domain.settlement.enums.SettlementBatchResult;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A2 배치 실행·이력(운영 콘솔) - 배치 이력 리스트/상세 공용 DTO
 *
 * 포함 이유(LOCKED):
 * - baseDate 재실행(SKIP) 정책 확인에 필요한 키: batchKey + runId
 * - OK/FAIL 판단: result + failReason
 * - Trace/Audit 재현: requestId + triggeredBy
 * - 운영 시간 확인: createdAt + finishedAt
 */
public record SettlementBatchSummaryResponse(
        Long batchId,
        LocalDate batchKey,          // baseDate
        String runId,
        SettlementBatchResult result, // OK/FAIL
        LocalDateTime createdAt,
        LocalDateTime finishedAt,
        String triggeredBy,
        String requestId,
        String failReason
) {}