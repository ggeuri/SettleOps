package com.settleops.domain.settlement.dto;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
/**
 * A2 배치 이력 통합 응답
 *
 * LOCKED:
 * - from/to(운영 안전 기간) + page(통합 row)
 */
public record SettlementBatchHistoryResponse(
        LocalDate from,
        LocalDate to,
        Page<SettlementBatchHistoryRowResponse> page
) {}