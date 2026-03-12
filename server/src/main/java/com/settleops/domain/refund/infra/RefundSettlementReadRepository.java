package com.settleops.domain.refund.infra;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.settleops.domain.refund.application.dto.ApprovedRefundAdjustment;
import com.settleops.domain.refund.domain.RefundStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.settleops.domain.refund.domain.QRefund.refund;
import static com.settleops.domain.refund.domain.QRefundSettlementLink.refundSettlementLink;

/**
 * READ 전용.
 *
 * LOCKED:
 * - APPROVED refund 중
 * - refund_settlement_link가 없는 미반영 건만
 * - decided_at < cutoffExclusive 인 건만 조회
 *
 * 주의:
 * - cutoffExclusive는 "baseDate 당일 00:00"을 넣는 것을 전제로 한다.
 */
@Repository
@RequiredArgsConstructor
public class RefundSettlementReadRepository {

    private final JPAQueryFactory queryFactory;

    public List<ApprovedRefundAdjustment> findApprovedUnlinkedRefundAdjustmentsBefore(LocalDateTime cutoffExclusive) {
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
}