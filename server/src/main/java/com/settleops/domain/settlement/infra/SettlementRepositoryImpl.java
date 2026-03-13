package com.settleops.domain.settlement.infra;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.settleops.domain.settlement.dto.AdminSettlementListItemResponse;
import com.settleops.domain.settlement.entity.QSettlement;
import com.settleops.domain.settlement.enums.SettlementStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SettlementRepositoryImpl implements SettlementRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<AdminSettlementListItemResponse> searchAdminSettlements(
            SettlementStatus status,
            String merchantId,
            Pageable pageable
    ) {
        QSettlement settlement = QSettlement.settlement;

        BooleanBuilder where = new BooleanBuilder();

        if (status != null) {
            where.and(settlement.status.eq(status));
        }

        if (StringUtils.hasText(merchantId)) {
            where.and(settlement.merchantId.eq(merchantId));
        }

        List<AdminSettlementListItemResponse> content = queryFactory
                .select(Projections.constructor(
                        AdminSettlementListItemResponse.class,
                        settlement.settlementId,
                        settlement.merchantId,
                        settlement.baseDate,
                        settlement.status,
                        settlement.gross,
                        settlement.fee,
                        settlement.vat,
                        settlement.net,
                        settlement.createdAt
                ))
                .from(settlement)
                .where(where)
                .orderBy(
                        settlement.baseDate.desc(),
                        settlement.createdAt.desc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        var countQuery = queryFactory
                .select(settlement.count())
                .from(settlement)
                .where(where);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
}