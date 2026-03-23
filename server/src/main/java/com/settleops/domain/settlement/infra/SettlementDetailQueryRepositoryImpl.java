package com.settleops.domain.settlement.infra;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.settleops.domain.refund.domain.QRefund;
import com.settleops.domain.refund.domain.RefundStatus;
import com.settleops.domain.settlement.dto.AdminSettlementDetailBaseView;
import com.settleops.domain.settlement.dto.AdminSettlementHoldSummaryResponse;
import com.settleops.domain.settlement.dto.AdminSettlementLineItemResponse;
import com.settleops.domain.hold.domain.QHold;
import com.settleops.domain.settlement.entity.QSettlement;
import com.settleops.domain.settlement.entity.QSettlementLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import com.settleops.global.audit.EntityType;
import com.settleops.global.audit.QAuditLog;

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
     * - refundAdjustmentPending 판정은 Repository에서 계산하지 않는다.
     * - pending SoT는 RefundAdjustmentPolicy 단일 구현이 담당한다.
     */
    @Override
    public boolean hasApprovedRefund(String settlementId) {
        QRefund refund = QRefund.refund;
        QSettlementLine settlementLine = QSettlementLine.settlementLine;

        return queryFactory
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
    }

    @Override
    public String findLatestNonNoOpSettlementRequestId(String settlementId) {
        QAuditLog auditLog = QAuditLog.auditLog;

        StringExpression noOpValue = Expressions.stringTemplate(
                "coalesce(function('json_unquote', function('json_extract', {0}, '$.noOp')), 'false')",
                auditLog.metaJson
        );

        return queryFactory
                .select(auditLog.requestId)
                .from(auditLog)
                .where(
                        auditLog.entityType.eq(EntityType.SETTLEMENT),
                        auditLog.entityId.eq(settlementId),
                        noOpValue.eq("false")
                )
                .orderBy(
                        auditLog.occurredAt.desc(),
                        auditLog.auditId.desc()
                )
                .fetchFirst();
    }
}