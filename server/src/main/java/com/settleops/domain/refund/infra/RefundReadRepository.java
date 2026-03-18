package com.settleops.domain.refund.infra;
import com.querydsl.core.BooleanBuilder;
import static com.settleops.domain.settlement.entity.QSettlementLine.settlementLine;
import static com.settleops.domain.refund.domain.RefundStatus.APPROVED;
import static com.settleops.domain.settlement.enums.SettlementLineType.PAYMENT;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.settleops.domain.refund.api.dto.AdminRefundListItemDTO;
import com.settleops.domain.refund.api.dto.QAdminRefundListItemDTO;
import com.settleops.domain.refund.domain.Refund;
import com.settleops.domain.refund.domain.RefundStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.settleops.domain.refund.domain.QRefund.refund;

@Repository
@RequiredArgsConstructor
public class RefundReadRepository {
    //(QueryDSL projection들)
    //admin 리스트/필터/기간 등
    private final JPAQueryFactory queryFactory;

    /**
     * 기존 request-paid/A 정책 호환용.
     * C의 refund_settlement_link SoT 경로에는 사용하지 않는다.
     */
    public boolean existsRefundSettlementLinkEvidence(String settlementId) {
        Integer one = queryFactory
                .selectOne()
                .from(settlementLine)
                .join(refund).on(refund.paymentId.eq(settlementLine.paymentId))
                .where(
                        settlementLine.settlementId.eq(settlementId),
                        settlementLine.lineType.eq(PAYMENT),
                        refund.status.eq(APPROVED)
                )
                .fetchFirst();

        return one != null;
    }

    /**
     * A6 운영 큐 조회(READ).
     * - status/from/to 옵션 조건으로 refund 목록을 조회한다.
     * - 시간 기준 SoT: refund.requestedAt (DB: requested_at)
     * - 정렬: requestedAt DESC (최신 요청 우선)
     */
    public List<AdminRefundListItemDTO> findAdminRefundQueue(
            RefundStatus status,
            LocalDateTime from,
            LocalDateTime to
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

        return queryFactory
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
                .fetch();
    }

    public List<Refund> findAllByOrderByCreatedAtDesc() {
        return queryFactory
                .selectFrom(refund)
                .orderBy(refund.createdAt.desc())
                .fetch();
    }
}
