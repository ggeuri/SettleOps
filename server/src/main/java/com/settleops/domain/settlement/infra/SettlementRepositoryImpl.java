package com.settleops.domain.settlement.infra;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.settleops.domain.settlement.dto.AdminSettlementListItemResponse;
import com.settleops.domain.settlement.dto.MerchantSettlementDetailResponse;
import com.settleops.domain.settlement.dto.MerchantSettlementLineItemResponse;
import com.settleops.domain.settlement.dto.MerchantSettlementListItemResponse;
import com.settleops.domain.settlement.entity.QSettlement;
import com.settleops.domain.settlement.entity.QSettlementLine;
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

    @Override
    public Page<MerchantSettlementListItemResponse> searchMerchantSettlements(String merchantId, Pageable pageable) {
        QSettlement settlement = QSettlement.settlement;

        List<MerchantSettlementListItemResponse> content = queryFactory
                .select(Projections.constructor(
                        MerchantSettlementListItemResponse.class,
                        settlement.settlementId,
                        settlement.baseDate,
                        settlement.status,
                        settlement.gross,
                        settlement.fee,
                        settlement.vat,
                        settlement.net,
                        settlement.createdAt
                ))
                .from(settlement)
                .where(settlement.merchantId.eq(merchantId))
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
                .where(settlement.merchantId.eq(merchantId));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public MerchantSettlementDetailResponse findMerchantSettlementDetail(String merchantId, String settlementId) {
        QSettlement settlement = QSettlement.settlement;
        QSettlementLine settlementLine = QSettlementLine.settlementLine;

        var base = queryFactory
                .select(Projections.constructor(
                        MerchantSettlementDetailResponse.class,
                        settlement.settlementId,
                        settlement.baseDate,
                        settlement.status,
                        settlement.gross,
                        settlement.fee,
                        settlement.vat,
                        settlement.net,
                        Expressions.constant(java.util.List.of())
                ))
                .from(settlement)
                .where(
                        settlement.merchantId.eq(merchantId),
                        settlement.settlementId.eq(settlementId)
                )
                .fetchOne();

        if (base == null) {
            return null;
        }

        List<MerchantSettlementLineItemResponse> lines = queryFactory
                .select(Projections.constructor(
                        MerchantSettlementLineItemResponse.class,
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

        return new MerchantSettlementDetailResponse(
                base.settlementId(),
                base.baseDate(),
                base.status(),
                base.gross(),
                base.fee(),
                base.vat(),
                base.net(),
                lines
        );
    }
}