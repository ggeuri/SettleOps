package com.settleops.domain.settlement.infra;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.settleops.domain.refund.domain.QRefund;
import com.settleops.domain.refund.domain.QRefundSettlementLink;
import com.settleops.domain.refund.domain.RefundStatus;
import com.settleops.domain.settlement.dto.AdminSettlementDetailBaseView;
import com.settleops.domain.settlement.dto.AdminSettlementHoldSummaryResponse;
import com.settleops.domain.settlement.dto.AdminSettlementLineItemResponse;
import com.settleops.domain.settlement.dto.AdminSettlementRefundSummaryResponse;
import com.settleops.domain.hold.entity.QHold;
import com.settleops.domain.settlement.entity.QSettlement;
import com.settleops.domain.settlement.entity.QSettlementLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SettlementDetailQueryRepositoryImpl implements SettlementDetailQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public AdminSettlementDetailBaseView findSettlementBase(String settlementId) {
        QSettlement settlement = QSettlement.settlement;

        return queryFactory
                .select(Projections.constructor(
                        AdminSettlementDetailBaseView.class,
                        settlement.settlementId,
                        settlement.merchantId,
                        settlement.baseDate,
                        settlement.status,
                        settlement.gross,
                        settlement.fee,
                        settlement.vat,
                        settlement.net
                ))
                .from(settlement)
                .where(settlement.settlementId.eq(settlementId))
                .fetchOne();
    }

    @Override
    public List<AdminSettlementLineItemResponse> findSettlementLines(String settlementId) {
        QSettlementLine settlementLine = QSettlementLine.settlementLine;

        return queryFactory
                .select(Projections.constructor(
                        AdminSettlementLineItemResponse.class,
                        settlementLine.settlementLineId.stringValue(),
                        settlementLine.lineType,
                        settlementLine.paymentId,
                        settlementLine.amount
                ))
                .from(settlementLine)
                .where(settlementLine.settlementId.eq(settlementId))
                .orderBy(
                        settlementLine.createdAt.asc(),
                        settlementLine.settlementLineId.asc()
                )
                .fetch();
    }

    @Override
    public AdminSettlementHoldSummaryResponse findHoldSummary(String settlementId) {
        QHold hold = QHold.hold;

        AdminSettlementHoldSummaryResponse result = queryFactory
                .select(Projections.constructor(
                        AdminSettlementHoldSummaryResponse.class,
                        Expressions.constant(true),
                        hold.holdId,
                        hold.status.stringValue(),
                        hold.requestedReasonCode.stringValue(),
                        hold.requestedComment,
                        hold.createdBy,
                        hold.createdAt
                ))
                .from(hold)
                .where(hold.settlementId.eq(settlementId))
                .fetchOne();

        return result != null ? result : AdminSettlementHoldSummaryResponse.empty();
    }

    /**
     * A4 refund summary 정책:
     * - hasApprovedRefund는 현재 settlement에 포함된 paymentId 기준으로 조회한다.
     * - refundAdjustmentPending의 SoT는 refund_settlement_link 이다.
     * - pending 판정은 refund_id가 refund_settlement_link에 존재하는지 여부만 사용한다.
     * - pending 판정에서는 settlement_id 조건으로 범위를 좁히지 않는다.
     */
    @Override
    public AdminSettlementRefundSummaryResponse findRefundSummary(String settlementId) {
        QRefund refund = QRefund.refund;
        QRefundSettlementLink refundSettlementLink = QRefundSettlementLink.refundSettlementLink;
        QSettlementLine settlementLine = QSettlementLine.settlementLine;

        boolean hasApprovedRefund = queryFactory
                .selectOne()
                .from(refund)
                .where(
                        refund.paymentId.in(
                                JPAExpressions
                                        .select(settlementLine.paymentId)
                                        .from(settlementLine)
                                        .where(settlementLine.settlementId.eq(settlementId))
                        ),
                        refund.status.eq(RefundStatus.APPROVED)
                )
                .fetchFirst() != null;

        boolean refundAdjustmentPending = queryFactory
                .selectOne()
                .from(refund)
                .where(
                        refund.paymentId.in(
                                JPAExpressions
                                        .select(settlementLine.paymentId)
                                        .from(settlementLine)
                                        .where(settlementLine.settlementId.eq(settlementId))
                        ),
                        refund.status.eq(RefundStatus.APPROVED),
                        refund.refundId.notIn(
                                JPAExpressions
                                        .select(refundSettlementLink.refundId)
                                        .from(refundSettlementLink)
                        )
                )
                .fetchFirst() != null;

        return new AdminSettlementRefundSummaryResponse(
                hasApprovedRefund,
                refundAdjustmentPending
        );
    }
}