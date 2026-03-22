package com.settleops.domain.refund.infra;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.settleops.domain.refund.api.dto.AdminRefundListItemDTO;
import com.settleops.domain.refund.api.dto.QAdminRefundListItemDTO;
import com.settleops.domain.refund.domain.Refund;
import com.settleops.domain.refund.domain.RefundStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.settleops.domain.refund.domain.QRefund.refund;


@Repository
@RequiredArgsConstructor
public class RefundReadRepository {
    //(QueryDSL projection들)
    //admin 리스트/필터/기간 등
    private final JPAQueryFactory queryFactory;

    /**
     * A6 운영 큐 조회(READ).
     * - status/from/to 옵션 조건으로 refund 목록을 조회한다.
     * - 시간 기준 SoT: refund.requestedAt (DB: requested_at)
     * - 정렬: requestedAt DESC (최신 요청 우선)
     */
    public Page<AdminRefundListItemDTO> findAdminRefundQueue(
            RefundStatus status,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        BooleanBuilder where = new BooleanBuilder();

        if (status != null) {
            where.and(refund.status.eq(status));
        }
        if (from != null) {
            where.and(refund.requestedAt.goe(from));
        }
        if (to != null) {
            where.and(refund.requestedAt.loe(to));
        }

        List<AdminRefundListItemDTO> content = queryFactory
                .select(new QAdminRefundListItemDTO(
                        refund.refundId,
                        refund.paymentId,
                        refund.merchantId,
                        refund.amount,
                        refund.status,
                        refund.requestedAt,
                        refund.decidedAt
                ))
                .from(refund)
                .where(where)
                .orderBy(refund.requestedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        var countQuery = queryFactory
                .select(refund.count())
                .from(refund)
                .where(where);

        return PageableExecutionUtils.getPage(
                content,
                pageable,
                () -> Optional.ofNullable(countQuery.fetchOne()).orElse(0L)
        );
    }

    public List<Refund> findAllByOrderByCreatedAtDesc() {
        return queryFactory
                .selectFrom(refund)
                .orderBy(refund.createdAt.desc())
                .fetch();
    }
}
