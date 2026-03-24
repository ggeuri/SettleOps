package com.settleops.domain.refund.infra;

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

    public String findLatestNonNoOpRefundRequestId(String refundId) {
        return queryFactory
                .select(auditLog.requestId)
                .from(auditLog)
                .where(
                        auditLog.entityType.eq(EntityType.REFUND),
                        auditLog.entityId.eq(refundId)
                )
                .orderBy(auditLog.occurredAt.desc(), auditLog.auditId.desc())
                .fetchFirst();
    }
}