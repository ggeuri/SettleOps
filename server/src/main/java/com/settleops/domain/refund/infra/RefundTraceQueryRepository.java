package com.settleops.domain.refund.infra;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.settleops.global.audit.EntityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import static com.settleops.domain.refund.domain.QRefund.refund;
import static com.settleops.global.audit.QAuditLog.auditLog;

@Repository
@RequiredArgsConstructor
public class RefundTraceQueryRepository {

    private final JPAQueryFactory queryFactory;

    public boolean existsRefund(String refundId) {
        Integer result = queryFactory
                .selectOne()
                .from(refund)
                .where(refund.refundId.eq(refundId))
                .fetchFirst();

        return result != null;
    }

    /**
     * refundId 기준으로 audit_log에서 최신 non-no-op requestId를 조회한다.
     * no-op 여부는 audit_log.meta_json.noOp=false 로 판정한다.
     */
    public String findLatestNonNoOpRefundRequestId(String refundId) {
        StringExpression noOpValue = Expressions.stringTemplate(
                "coalesce(function('json_unquote', function('json_extract', {0}, '$.noOp')), 'false')",
                auditLog.metaJson
        );

        return queryFactory
                .select(auditLog.requestId)
                .from(auditLog)
                .where(
                        auditLog.entityType.eq(EntityType.REFUND),
                        auditLog.entityId.eq(refundId),
                        noOpValue.eq("false")
                )
                .orderBy(
                        auditLog.occurredAt.desc(),
                        auditLog.auditId.desc()
                )
                .fetchFirst();
    }
}