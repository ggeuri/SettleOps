package com.settleops.domain.refund.infra;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.settleops.domain.refund.domain.Refund;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.settleops.domain.refund.domain.QRefund.refund;
import static com.settleops.domain.settlement.entity.QSettlementLine.settlementLine;
import static com.settleops.domain.refund.domain.RefundStatus.APPROVED;
import static com.settleops.domain.settlement.enums.SettlementLineType.PAYMENT;

@Repository
@RequiredArgsConstructor
public class RefundReadRepository {
    //(QueryDSL projection들)
    //admin 리스트/필터/기간 등
    private final JPAQueryFactory queryFactory;

    public boolean existsApprovedRefundBySettlementId(String settlementId) {
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

    public List<Refund> findAllByOrderByCreatedAtDesc() {
        return queryFactory
                .selectFrom(refund)
                .orderBy(refund.createdAt.desc())
                .fetch(); // 결과 없으면 빈 리스트 반환 (null 아님)
    }
}
