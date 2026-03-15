package com.settleops.domain.refund.infra;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.settleops.domain.refund.domain.QRefundSettlementLink;
import com.settleops.domain.refund.domain.RefundStatus;
import com.settleops.domain.settlement.entity.QSettlementLine;
import com.settleops.domain.settlement.enums.SettlementLineType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import static com.settleops.domain.refund.domain.QRefund.refund;

/**
 * READ 전용.
 *
 * 책임:
 * - settlementId에 연결된 PAYMENT line들의 paymentId를 기준으로
 *   APPROVED refund 중 아직 refund_settlement_link가 없는 건이 존재하는지 확인
 *
 * 주의:
 * - "반영 완료" 증거는 refund_settlement_link만 사용
 * - settlement_line 존재만으로 반영 완료 추론 금지
 */
@Repository
@RequiredArgsConstructor
public class RefundAdjustmentPendingReadRepository {

    private final JPAQueryFactory queryFactory;

    public boolean existsPendingApprovedRefundForSettlement(String settlementId) {
        QSettlementLine paymentLine = new QSettlementLine("paymentLine");
        QRefundSettlementLink link = new QRefundSettlementLink("link");

        Integer one = queryFactory
                .selectOne()
                .from(refund)
                .where(
                        refund.status.eq(RefundStatus.APPROVED),
                        refund.decidedAt.isNotNull(),

                        JPAExpressions
                                .selectOne()
                                .from(paymentLine)
                                .where(
                                        paymentLine.settlementId.eq(settlementId),
                                        paymentLine.lineType.eq(SettlementLineType.PAYMENT),
                                        paymentLine.paymentId.eq(refund.paymentId)
                                )
                                .exists(),

                        JPAExpressions
                                .selectOne()
                                .from(link)
                                .where(link.refundId.eq(refund.refundId))
                                .notExists()
                )
                .fetchFirst();

        return one != null;
    }
}