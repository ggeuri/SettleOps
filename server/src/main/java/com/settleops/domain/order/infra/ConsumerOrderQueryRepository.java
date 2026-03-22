package com.settleops.domain.order.infra;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.settleops.domain.order.api.dto.ConsumerOrderDetailResponse;
import com.settleops.domain.payment.domain.QPaymentEvent;
import com.settleops.domain.payment.domain.PaymentEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.settleops.domain.order.domain.QOrders.orders;
import static com.settleops.domain.payment.domain.QPayment.payment;
import static com.settleops.domain.payment.domain.QPaymentEvent.paymentEvent;

import com.querydsl.core.BooleanBuilder;
import com.settleops.domain.order.api.dto.ConsumerOrderListItemResponse;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class ConsumerOrderQueryRepository {

    private final JPAQueryFactory queryFactory;

    public ConsumerOrderDetailResponse findConsumerOrderDetail(String orderId) {
        QPaymentEvent capturedEvent = new QPaymentEvent("capturedEvent");
        QPaymentEvent confirmedEvent = new QPaymentEvent("confirmedEvent");

        ConsumerOrderFlatRow row = queryFactory
                .select(Projections.constructor(
                        ConsumerOrderFlatRow.class,
                        orders.orderId,
                        orders.merchantId,
                        orders.buyerId,
                        orders.itemName,
                        orders.amount,
                        orders.status.stringValue(),
                        payment.paymentId,
                        payment.status.stringValue(),
                        capturedEvent.occurredAt,
                        confirmedEvent.occurredAt
                ))
                .from(orders)
                .leftJoin(payment).on(payment.orderId.eq(orders.orderId))
                .leftJoin(capturedEvent).on(
                        capturedEvent.paymentId.eq(payment.paymentId)
                                .and(capturedEvent.eventType.eq(PaymentEventType.PAYMENT_CAPTURED))
                )
                .leftJoin(confirmedEvent).on(
                        confirmedEvent.paymentId.eq(payment.paymentId)
                                .and(confirmedEvent.eventType.eq(PaymentEventType.PAYMENT_CONFIRMED))
                )
                .where(orders.orderId.eq(orderId))
                .fetchOne();

        if (row == null) {
            return null;
        }

        List<ConsumerOrderDetailResponse.PaymentEventItem> events =
                row.paymentId() == null ? List.of() : findPaymentEvents(row.paymentId());

        return new ConsumerOrderDetailResponse(
                row.orderId(),
                row.merchantId(),
                row.buyerId(),
                row.itemName(),
                row.amount(),
                row.orderStatus(),
                row.paymentId(),
                row.paymentStatus(),
                row.capturedAt(),
                row.confirmedAt(),
                events
        );
    }

    private List<ConsumerOrderDetailResponse.PaymentEventItem> findPaymentEvents(String paymentId) {
        return queryFactory
                .select(Projections.constructor(
                        ConsumerOrderDetailResponse.PaymentEventItem.class,
                        paymentEvent.eventType.stringValue(),
                        paymentEvent.statusBefore.stringValue(),
                        paymentEvent.statusAfter.stringValue(),
                        paymentEvent.occurredAt
                ))
                .from(paymentEvent)
                .where(paymentEvent.paymentId.eq(paymentId))
                .orderBy(paymentEvent.occurredAt.asc(), paymentEvent.paymentEventId.asc())
                .fetch();
    }

    public List<ConsumerOrderListItemResponse> findConsumerOrders(
            String buyerId,
            String status,
            String keyword
    ) {
        QPaymentEvent confirmedEvent = new QPaymentEvent("confirmedEvent");

        BooleanBuilder condition = new BooleanBuilder()
                .and(orders.buyerId.eq(buyerId));

        if (StringUtils.hasText(status)) {
            condition.and(orders.status.stringValue().eq(status));
        }

        if (StringUtils.hasText(keyword)) {
            condition.and(
                    orders.orderId.containsIgnoreCase(keyword)
                            .or(orders.itemName.containsIgnoreCase(keyword))
            );
        }

        List<ConsumerOrderListFlatRow> rows = queryFactory
                .select(Projections.constructor(
                        ConsumerOrderListFlatRow.class,
                        orders.orderId,
                        orders.buyerId,
                        orders.itemName,
                        orders.amount,
                        orders.status.stringValue(),
                        payment.paymentId,
                        confirmedEvent.occurredAt,
                        orders.createdAt
                ))
                .from(orders)
                .leftJoin(payment).on(payment.orderId.eq(orders.orderId))
                .leftJoin(confirmedEvent).on(
                        confirmedEvent.paymentId.eq(payment.paymentId)
                                .and(confirmedEvent.eventType.eq(PaymentEventType.PAYMENT_CONFIRMED))
                )
                .where(condition)
                .orderBy(orders.createdAt.desc())
                .fetch();

        return rows.stream()
                .map(row -> new ConsumerOrderListItemResponse(
                        row.orderId(),
                        row.itemName(),
                        row.amount(),
                        row.orderStatus(),
                        row.paymentId() != null,
                        row.confirmedAt() != null,
                        row.createdAt()
                ))
                .toList();
    }
}