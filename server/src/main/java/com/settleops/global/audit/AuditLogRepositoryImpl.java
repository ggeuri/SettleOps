package com.settleops.global.audit;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.settleops.global.enums.Action;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.querydsl.core.types.dsl.Expressions.allOf;
import static com.settleops.global.audit.QAuditLog.auditLog;

//AuditLog를 requestId/merchantId(+entityType/+기간) 조건으로 occurredAt DESC 정렬 + 페이징해서 조회하는 QueryDSL 구현체.

@Repository
@RequiredArgsConstructor
public class AuditLogRepositoryImpl implements  AuditLogQuery{
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Page<AuditLog> findByRequestIdOrderByOccurredAtDesc(String requestId, boolean includeNoOp,Pageable pageable) {

        return page(auditLog.requestId.eq(requestId), null,null, noOpCondition(includeNoOp), pageable);
    }

    @Override
    public Page<AuditLog> findByRequestIdAndEntityTypeOrderByOccurredAtDesc(String requestId, EntityType entityType, boolean includeNoOp,Pageable pageable) {
        return page(auditLog.requestId.eq(requestId),auditLog.entityType.eq(entityType),null,  noOpCondition(includeNoOp), pageable);
    }

    @Override
    public Page<AuditLog> findByMerchantIdAndOccurredAtBetweenOrderByOccurredAtDesc(String merchantId, LocalDateTime from, LocalDateTime to, boolean includeNoOp,Pageable pageable) {
        return page(auditLog.merchantId.eq(merchantId),null,auditLog.occurredAt.between(from, to), noOpCondition(includeNoOp), pageable);
    }

    @Override
    public Page<AuditLog> findByMerchantIdAndEntityTypeAndOccurredAtBetweenOrderByOccurredAtDesc(String merchantId, EntityType entityType, LocalDateTime from, LocalDateTime to, boolean includeNoOp,Pageable pageable) {
        return page(auditLog.merchantId.eq(merchantId),auditLog.entityType.eq(entityType),auditLog.occurredAt.between(from, to), noOpCondition(includeNoOp),pageable);
    }

    // occurredAt DESC 고정 + pageable offset/limit 적용 + countQuery로 Page 구성

    private Page<AuditLog> page(BooleanExpression keyCond,
                                BooleanExpression entityTypeCond,
                                BooleanExpression rangeCond,
                                BooleanExpression noOpCond,
                                Pageable pageable) {

        // Expressions.allOf는 null-safe
        BooleanExpression whereCond = allOf(keyCond, entityTypeCond, rangeCond, noOpCond);

        List<AuditLog> content = jpaQueryFactory
                .selectFrom(auditLog)
                .where(whereCond)
                .orderBy(auditLog.occurredAt.desc(), auditLog.auditId.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        var countQuery = jpaQueryFactory
                .select(auditLog.count())
                .from(auditLog)
                .where(whereCond);

        // countQuery는 "필요할 때만" 실행되도록 lazy 처리
        return PageableExecutionUtils.getPage(content,pageable,() -> Optional.ofNullable(countQuery.fetchOne()).orElse(0L));

    }

    /**
     * A2 (Batch SKIP) 전용: action + entityType(BATCH) + 기간
     */
    @Override
    public Page<AuditLog> findByActionAndEntityTypeAndOccurredAtBetweenOrderByOccurredAtDesc(
            Action action,
            EntityType entityType,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        return pageWithAction(
                null, // A2 SKIP 목록: requestId/merchantId 같은 keyCond 없음
                auditLog.entityType.eq(entityType),
                auditLog.action.eq(action),
                auditLog.occurredAt.between(from, to),
                pageable
        );
    }

    private BooleanExpression noOpCondition(boolean includeNoOp) {
        if (includeNoOp) {
            return null;
        }

        return Expressions.booleanTemplate(
                "coalesce(json_extract({0}, '$.noOp') = true, false) = false",
                auditLog.metaJson
        );
    }

    // A2(SKIP) 전용: actionCond 포함
    private Page<AuditLog> pageWithAction(
            BooleanExpression keyCond,
            BooleanExpression entityTypeCond,
            BooleanExpression actionCond,
            BooleanExpression rangeCond,
            Pageable pageable
    ) {
        BooleanExpression whereCond = allOf(keyCond, entityTypeCond, actionCond, rangeCond);

        List<AuditLog> content = jpaQueryFactory
                .selectFrom(auditLog)
                .where(whereCond)
                .orderBy(auditLog.occurredAt.desc(), auditLog.auditId.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        var countQuery = jpaQueryFactory
                .select(auditLog.count())
                .from(auditLog)
                .where(whereCond);

        return PageableExecutionUtils.getPage(
                content,
                pageable,
                () -> Optional.ofNullable(countQuery.fetchOne()).orElse(0L)
        );
    }
}
