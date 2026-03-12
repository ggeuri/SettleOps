package com.settleops.domain.refund.application;

import com.settleops.domain.refund.application.dto.ApprovedRefundAdjustment;

import java.time.LocalDate;
import java.util.List;

/**
 * Refund → Settlement 연결부.
 *
 * 책임:
 * - APPROVED refund 중 아직 refund_settlement_link가 없는 건을
 *   다음 배치용 REFUND line 입력 집합으로 산출한다.
 *
 * 규칙:
 * - baseDate 배치 대상은 baseDate 당일 00:00 이전에 승인 완료된 refund만 포함한다.
 *
 * 비책임:
 * - settlement_line insert
 * - refund_settlement_link insert
 * - batch 내부 재판정
 */
public interface RefundSettlementAssembler {

    List<ApprovedRefundAdjustment> getApprovedRefundAdjustmentsForBaseDate(LocalDate baseDate);
}