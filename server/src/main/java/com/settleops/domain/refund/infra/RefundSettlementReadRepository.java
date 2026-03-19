package com.settleops.domain.refund.infra;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.settleops.domain.refund.application.dto.ApprovedRefundAdjustment;
import com.settleops.domain.refund.domain.RefundStatus;
import com.settleops.domain.settlement.enums.SettlementLineType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.settleops.domain.refund.domain.QRefund.refund;
import static com.settleops.domain.refund.domain.QRefundSettlementLink.refundSettlementLink;
import static com.settleops.domain.settlement.entity.QSettlementLine.settlementLine;

/**
 * READ 전용.
 *
 * LOCKED 해석:
 * 1) approved refund 존재 여부는 refund 테이블에서 판단한다.
 * 2) 반영 완료 증거는 refund_settlement_link 존재 여부로만 판단한다.
 * 3) settlement_line/payment 기반 조인 추론은 사용하지 않는다.
 */
@Repository
@RequiredArgsConstructor
public class RefundSettlementReadRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 다음 배치 입력집합 산출용.
     *
     * 조건:
     * - refund.status = APPROVED
     * - refund.decidedAt < cutoffExclusive
     * - refund_settlement_link 없음(= 아직 어떤 settlement에도 반영되지 않음)
     */

    public List<ApprovedRefundAdjustment> findApprovedRefundsWithoutSettlementLinkBefore(
            LocalDateTime cutoffExclusive
    ) {
        return queryFactory
                .select(Projections.constructor(
                        ApprovedRefundAdjustment.class,
                        refund.refundId,
                        refund.paymentId,
                        refund.merchantId,
                        refund.amount,
                        refund.decidedAt
                ))
                .from(refund)
                .leftJoin(refundSettlementLink)
                .on(refundSettlementLink.refundId.eq(refund.refundId))
                .where(
                        refund.status.eq(RefundStatus.APPROVED),
                        refund.decidedAt.isNotNull(),
                        refund.decidedAt.lt(cutoffExclusive),
                        refundSettlementLink.refundId.isNull()
                )
                .orderBy(
                        refund.decidedAt.asc(),
                        refund.refundId.asc()
                )
                .fetch();
    }

    /**
     * 개별 refundId 기준 반영 증거 확인용.
     * refund_settlement_link insert 존재 여부만 확인한다.
     */
    public boolean existsSettlementLinkByRefundId(String refundId) {
        Integer one = queryFactory
                .selectOne()
                .from(refundSettlementLink)
                .where(refundSettlementLink.refundId.eq(refundId))
                .fetchFirst();

        return one != null;
    }

    /**
     * LOCKED 해석:
     * - settlementId 기준 refund 후보 식별은 settlement_line(PAYMENT) / payment_id 연결로 수행한다.
     * - 승인 여부는 refund.status=APPROVED 기준으로 판단한다.
     * - 반영 완료 여부는 refund_settlement_link 존재 여부로만 판단한다.
     * - settlement_line/payment_id 기반 조인으로 "반영 완료"를 추론하지 않는다.
     */
    public boolean existsPendingApprovedRefundBySettlementId(String settlementId) {
        Integer one = queryFactory
                .selectOne()
                .from(settlementLine)
                .join(refund)
                .on(refund.paymentId.eq(settlementLine.paymentId))
                .leftJoin(refundSettlementLink)
                .on(refundSettlementLink.refundId.eq(refund.refundId))
                .where(
                        settlementLine.settlementId.eq(settlementId),
                        settlementLine.lineType.eq(SettlementLineType.PAYMENT),
                        refund.status.eq(RefundStatus.APPROVED),
                        refundSettlementLink.refundId.isNull()
                )
                .fetchFirst();

        return one != null;
    }
}