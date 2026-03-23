package com.settleops.domain.hold.infra;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.settleops.domain.hold.api.dto.AdminHoldQueueRowDto;
import com.settleops.domain.hold.api.dto.HoldQueueSearchRequestDto;
import com.settleops.domain.hold.domain.QHold;
import com.settleops.domain.settlement.entity.QSettlement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class HoldQueryRepositoryImpl implements HoldQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<AdminHoldQueueRowDto> search(HoldQueueSearchRequestDto request, Pageable pageable) {
        QHold hold = QHold.hold;
        QSettlement settlement = QSettlement.settlement;

        BooleanBuilder builder = new BooleanBuilder();

        if (request.status() != null) {
            builder.and(hold.status.eq(request.status()));
        }
        if (request.settlementId() != null && !request.settlementId().isBlank()) {
            builder.and(hold.settlementId.eq(request.settlementId()));
        }
        if (request.merchantId() != null && !request.merchantId().isBlank()) {
            builder.and(settlement.merchantId.eq(request.merchantId()));
        }

        List<AdminHoldQueueRowDto> content = queryFactory
                .select(Projections.constructor(
                        AdminHoldQueueRowDto.class,
                        hold.holdId,
                        hold.settlementId,
                        settlement.merchantId,
                        hold.status,
                        hold.requestedReasonCode,
                        hold.requestedComment,
                        hold.createdAt
                ))
                .from(hold)
                .join(settlement).on(settlement.settlementId.eq(hold.settlementId))
                .where(builder)
                .orderBy(hold.createdAt.desc(), hold.holdId.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(hold.count())
                .from(hold)
                .join(settlement).on(settlement.settlementId.eq(hold.settlementId))
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }
}