package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.dto.SettlementBatchHistoryResponse;
import com.settleops.domain.settlement.dto.SettlementBatchSkipResponse;
import com.settleops.domain.settlement.dto.SettlementBatchSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface SettlementBatchQueryService {
    /**
     * A2 배치 실행·이력(OK/FAIL + SKIP) 통합 조회
     *
     * LOCKED:
     * - SKIP 이력은 audit_log(BATCH_RUN_SKIPPED) 기반
     * - OK/FAIL 이력은 settlement_batch 기반
     */
    SettlementBatchHistoryResponse getHistory(LocalDate from, LocalDate to, Pageable pageable);
}