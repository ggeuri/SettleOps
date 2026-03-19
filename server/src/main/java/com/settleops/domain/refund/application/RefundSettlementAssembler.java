package com.settleops.domain.refund.application;

import com.settleops.domain.refund.application.dto.ApprovedRefundAdjustment;

import java.time.LocalDate;
import java.util.List;

/**
 * Refund → Settlement 반영 대상 입력집합 산출 계약.
 *
 * 책임(LOCKED):
 * - 승인 사실 SoT는 refund(status=APPROVED) 로 판단한다.
 * - 반영 완료 증거 SoT는 refund_settlement_link 존재 여부로 판단한다.
 * - APPROVED 이면서 refund_settlement_link가 아직 없는 refund만
 *   다음 배치의 REFUND line 입력 집합으로 산출한다.
 *
 * 배치 기준(LOCKED):
 * - baseDate 배치 대상은 baseDate 당일 00:00 이전에 승인 완료된 refund만 포함한다.
 *
 * 비책임(LOCKED):
 * - settlement_line insert
 * - refund_settlement_link insert
 * - batch 내부 재판정
 * - settlement_line / payment_id 기반 조인으로 반영 완료를 추론하는 판단
 */
public interface RefundSettlementAssembler {

    List<ApprovedRefundAdjustment> getApprovedRefundAdjustmentsForBaseDate(LocalDate baseDate);
}