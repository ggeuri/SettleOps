package com.settleops.domain.refund.application;

import com.settleops.domain.refund.application.dto.ApprovedRefundAdjustment;

import java.time.LocalDate;
import java.util.List;

/**
 * Refund -> Settlement 입력집합 산출.
 *
 * 책임:
 * - APPROVED refund 중 아직 refund_settlement_link가 없는 건을
 *   다음 배치 REFUND line 입력집합으로 산출한다.
 *
 * LOCKED:
 * - 기준 경계는 KST(Asia/Seoul) baseDate 당일 00:00 이다.
 * - 포함 대상은 baseDate 당일 00:00 "이전" 승인 완료건만 해당한다.
 * - cutoff 비교는 decidedAt < baseDate.atStartOfDay() 이다.
 *
 * 비책임:
 * - settlement_line(REFUND) insert
 * - refund_settlement_link insert
 * - request-paid 차단/해제 판정
 */
public interface RefundSettlementAssembler {
    List<ApprovedRefundAdjustment> getApprovedRefundAdjustmentsForBaseDate(LocalDate baseDate);
}