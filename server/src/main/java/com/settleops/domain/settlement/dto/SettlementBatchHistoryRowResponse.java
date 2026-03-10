package com.settleops.domain.settlement.dto;

import java.time.LocalDateTime;


/**
 * A2 배치 이력 통합 row (OK/FAIL + SKIP)
 *
 * LOCKED:
 * - 한 화면에서 시간축(occurredAt) 기준으로 합쳐 보여줌
 * - type 으로 OK_FAIL vs SKIP 구분
 */
public record SettlementBatchHistoryRowResponse(
        RowType type,                 // OK_FAIL or SKIP
        LocalDateTime occurredAt,      // 정렬 기준(내림차순)
        SettlementBatchSummaryResponse batch,   // OK/FAIL 상세(없으면 null)
        SettlementBatchSkipResponse skip        // SKIP 상세(없으면 null)
) {
    public enum RowType { OK_FAIL, SKIP }

    public static SettlementBatchHistoryRowResponse okFail(LocalDateTime occurredAt, SettlementBatchSummaryResponse batch) {
        return new SettlementBatchHistoryRowResponse(RowType.OK_FAIL, occurredAt, batch, null);
    }

    public static SettlementBatchHistoryRowResponse skip(LocalDateTime occurredAt, SettlementBatchSkipResponse skip) {
        return new SettlementBatchHistoryRowResponse(RowType.SKIP, occurredAt, null, skip);
    }
}