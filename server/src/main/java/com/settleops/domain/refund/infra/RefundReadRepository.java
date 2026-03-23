package com.settleops.domain.refund.infra;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.settleops.domain.payment.domain.QPayment;
import com.settleops.domain.refund.api.dto.AdminRefundListItemDTO;
import com.settleops.domain.refund.api.dto.QAdminRefundListItemDTO;
import com.settleops.domain.refund.api.dto.QRefundRowDTO;
import com.settleops.domain.refund.api.dto.RefundRowDTO;
import com.settleops.domain.refund.domain.QRefund;
import com.settleops.domain.refund.domain.RefundStatus;
import com.settleops.domain.settlement.entity.QSettlementLine;
import com.settleops.domain.settlement.enums.SettlementLineType;
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

    private final JPAQueryFactory queryFactory;

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

        QPayment payment = QPayment.payment;
        QSettlementLine settlementLine = QSettlementLine.settlementLine;
        QRefund approvedRefund = new QRefund("approvedRefund");

        List<AdminRefundListItemDTO> content =
                queryFactory
                        .select(new QAdminRefundListItemDTO(
                                refund.refundId,
                                refund.paymentId,
                                JPAExpressions
                                        .select(settlementLine.settlementId.max())
                                        .from(settlementLine)
                                        .where(
                                                settlementLine.paymentId.eq(refund.paymentId),
                                                settlementLine.lineType.eq(SettlementLineType.PAYMENT)
                                        ),
                                refund.merchantId,
                                refund.amount,
                                payment.capturedAmount,
                                payment.capturedAmount.subtract(
                                        JPAExpressions
                                                .select(approvedRefund.amount.sum().coalesce(0L))
                                                .from(approvedRefund)
                                                .where(
                                                        approvedRefund.paymentId.eq(refund.paymentId),
                                                        approvedRefund.status.eq(RefundStatus.APPROVED)
                                                )
                                ),
                                refund.status,
                                refund.reasonText,
                                refund.requestedAt,
                                refund.decidedAt
                        ))
                        .from(refund)
                        .join(payment).on(payment.paymentId.eq(refund.paymentId))
                        .where(where)
                        .orderBy(refund.requestedAt.desc())
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .fetch();

        var countQuery =
                queryFactory
                        .select(refund.count())
                        .from(refund)
                        .where(where);

        return PageableExecutionUtils.getPage(
                content,
                pageable,
                () -> Optional.ofNullable(countQuery.fetchOne()).orElse(0L)
        );
    }

    public Page<RefundRowDTO> findMyRefunds(
            String loginMerchantId,
            RefundStatus status,
            LocalDateTime from,
            LocalDateTime to,
            String keyword,
            String sortKey,
            String sortDirection,
            Pageable pageable
    ) {
        BooleanBuilder where = new BooleanBuilder();

        where.and(refund.merchantId.eq(loginMerchantId));

        if (status != null) {
            where.and(refund.status.eq(status));
        }

        if (from != null) {
            where.and(refund.requestedAt.goe(from));
        }

        if (to != null) {
            where.and(refund.requestedAt.loe(to));
        }

        if (keyword != null && !keyword.isBlank()) {
            String trimmedKeyword = keyword.trim();
            where.and(
                    refund.refundId.containsIgnoreCase(trimmedKeyword)
                            .or(refund.paymentId.containsIgnoreCase(trimmedKeyword))
            );
        }

        List<RefundRowDTO> content =
                queryFactory
                        .select(new QRefundRowDTO(
                                refund.refundId,
                                refund.paymentId,
                                refund.merchantId,
                                refund.amount,
                                refund.status,
                                refund.reasonText,
                                refund.requestedAt,
                                refund.decidedAt
                        ))
                        .from(refund)
                        .where(where)
                        .orderBy(resolveSort(sortKey, sortDirection))
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .fetch();

        var countQuery =
                queryFactory
                        .select(refund.count())
                        .from(refund)
                        .where(where);

        return PageableExecutionUtils.getPage(
                content,
                pageable,
                () -> Optional.ofNullable(countQuery.fetchOne()).orElse(0L)
        );
    }

    private OrderSpecifier<?> resolveSort(String sortKey, String sortDirection) {
        Order order = "asc".equalsIgnoreCase(sortDirection) ? Order.ASC : Order.DESC;

        ComparableExpressionBase<?> expression = switch (sortKey) {
            case "refundId" -> refund.refundId;
            case "paymentId" -> refund.paymentId;
            case "amount" -> refund.amount;
            case "status" -> refund.status;
            case "decidedAt" -> refund.decidedAt;
            case "requestedAt" -> refund.requestedAt;
            default -> refund.requestedAt;
        };

        return new OrderSpecifier<>(order, expression);
    }
}