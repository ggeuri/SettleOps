package com.settleops.domain.payment.infra;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.settleops.domain.order.domain.QOrders;
import com.settleops.domain.payment.api.dto.MerchantPaymentListItemResponse;
import com.settleops.domain.payment.api.dto.MerchantPaymentSearchCondition;
import com.settleops.domain.payment.api.dto.PaymentDetailResponse;
import com.settleops.domain.payment.api.dto.RefundContextResponse;
import com.settleops.domain.payment.domain.PaymentEventType;
import com.settleops.domain.payment.domain.QPayment;
import com.settleops.domain.payment.domain.QPaymentEvent;
import com.settleops.domain.refund.domain.QRefund;
import com.settleops.domain.refund.domain.RefundStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PaymentQueryRepository {

    /** U2 확정여부 필터 기본값 */
    private static final String CONFIRMED_ALL = "ALL";

    /** U2 확정여부 필터 - 확정 건만 조회 */
    private static final String CONFIRMED_ONLY = "CONFIRMED";

    private final JPAQueryFactory queryFactory;

    /**
     * Merchant 결제 목록 조회
     * - merchantId 기준 기본 범위 제한
     * - 상태 / 확정여부 / 기간 / 키워드 조건 반영
     * - confirmed / confirmedAt 은 PAYMENT_CONFIRMED 이벤트 존재 여부로 파생
     */
    public List<MerchantPaymentListItemResponse> searchMerchantPayments(
            String merchantId,
            MerchantPaymentSearchCondition condition
    ) {
        QPayment payment = QPayment.payment;
        QOrders order = QOrders.orders;
        QPaymentEvent confirmedEvent = new QPaymentEvent("confirmedEvent");
        QPaymentEvent confirmedAtEvent = new QPaymentEvent("confirmedAtEvent");
        QPaymentEvent capturedAtEvent = new QPaymentEvent("capturedAtEvent");

        BooleanBuilder where = new BooleanBuilder();
        where.and(payment.merchantId.eq(merchantId));
        where.and(statusEq(condition.getStatus(), payment));
        where.and(confirmedFilter(condition.getConfirmed(), payment, confirmedEvent));
        where.and(createdAtGoe(condition, payment));
        where.and(createdAtLt(condition, payment));
        where.and(keywordContains(condition, payment, order));

        return queryFactory
                .select(Projections.constructor(
                        MerchantPaymentListItemResponse.class,
                        payment.paymentId,
                        payment.orderId,
                        payment.status.stringValue(),
                        payment.requestedAmount,
                        payment.capturedAmount,
                        payment.currency,
                        payment.buyerId,
                        JPAExpressions
                                .select(capturedAtEvent.occurredAt.min())
                                .from(capturedAtEvent)
                                .where(
                                        capturedAtEvent.paymentId.eq(payment.paymentId),
                                        capturedAtEvent.eventType.eq(PaymentEventType.PAYMENT_CAPTURED)
                                ),
                        JPAExpressions
                                .selectOne()
                                .from(confirmedEvent)
                                .where(
                                        confirmedEvent.paymentId.eq(payment.paymentId),
                                        confirmedEvent.eventType.eq(PaymentEventType.PAYMENT_CONFIRMED)
                                )
                                .exists(),
                        JPAExpressions
                                .select(confirmedAtEvent.occurredAt.min())
                                .from(confirmedAtEvent)
                                .where(
                                        confirmedAtEvent.paymentId.eq(payment.paymentId),
                                        confirmedAtEvent.eventType.eq(PaymentEventType.PAYMENT_CONFIRMED)
                                )
                ))
                .from(payment)
                .join(order).on(payment.orderId.eq(order.orderId))
                .where(where)
                .orderBy(payment.createdAt.desc())
                .fetch();
    }

    /**
     * 결제 상세 조회
     * - payment 기본 정보 조회
     * - capturedAt은 PAYMENT_CAPTURED 이벤트 occurredAt 파생
     * - confirmed / confirmedAt은 PAYMENT_CONFIRMED 이벤트 기반 파생
     * - events 타임라인은 별도 조회 후 조립
     */
    public PaymentDetailResponse findPaymentDetail(String paymentId) {
        QPayment payment = QPayment.payment;
        QPaymentEvent confirmedEvent = new QPaymentEvent("confirmedEvent");
        QPaymentEvent confirmedAtEvent = new QPaymentEvent("confirmedAtEvent");
        QPaymentEvent capturedAtEvent = new QPaymentEvent("capturedAtEvent");

        PaymentDetailResponse detail = queryFactory
                .select(Projections.constructor(
                        PaymentDetailResponse.class,
                        payment.paymentId,
                        payment.orderId,
                        payment.merchantId,
                        payment.buyerId,
                        payment.status.stringValue(),
                        payment.requestedAmount,
                        payment.capturedAmount,
                        payment.currency,
                        payment.createdAt,
                        JPAExpressions
                                .select(capturedAtEvent.occurredAt.min())
                                .from(capturedAtEvent)
                                .where(
                                        capturedAtEvent.paymentId.eq(payment.paymentId),
                                        capturedAtEvent.eventType.eq(PaymentEventType.PAYMENT_CAPTURED)
                                ),
                        JPAExpressions
                                .selectOne()
                                .from(confirmedEvent)
                                .where(
                                        confirmedEvent.paymentId.eq(payment.paymentId),
                                        confirmedEvent.eventType.eq(PaymentEventType.PAYMENT_CONFIRMED)
                                )
                                .exists(),
                        JPAExpressions
                                .select(confirmedAtEvent.occurredAt.min())
                                .from(confirmedAtEvent)
                                .where(
                                        confirmedAtEvent.paymentId.eq(payment.paymentId),
                                        confirmedAtEvent.eventType.eq(PaymentEventType.PAYMENT_CONFIRMED)
                                )
                ))
                .from(payment)
                .where(payment.paymentId.eq(paymentId))
                .fetchOne();

        if (detail == null) {
            return null;
        }

        detail.assignEvents(findPaymentEvents(paymentId));
        return detail;
    }

    /**
     * 결제 이벤트 타임라인 조회
     * - payment_event insert-only 이력 조회
     * - occurredAt 오름차순 정렬
     * - 상세 화면 타임라인 표시 목적
     */
    private List<PaymentDetailResponse.PaymentEventItem> findPaymentEvents(String paymentId) {
        QPaymentEvent paymentEvent = QPaymentEvent.paymentEvent;

        return queryFactory
                .select(Projections.constructor(
                        PaymentDetailResponse.PaymentEventItem.class,
                        paymentEvent.eventType.stringValue(),
                        paymentEvent.statusBefore.stringValue(),
                        paymentEvent.statusAfter.stringValue(),
                        paymentEvent.occurredAt
                ))
                .from(paymentEvent)
                .where(paymentEvent.paymentId.eq(paymentId))
                .orderBy(paymentEvent.occurredAt.asc())
                .fetch();
    }

    /**
     * 상태 필터 조건 생성
     * - null / blank / ALL 입력 시 조건 미적용
     */
    private BooleanBuilder statusEq(String status, QPayment payment) {
        BooleanBuilder builder = new BooleanBuilder();

        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return builder;
        }

        builder.and(payment.status.stringValue().eq(status));
        return builder;
    }

    /**
     * 확정여부 필터 조건 생성
     * - ALL: 조건 미적용
     * - CONFIRMED: PAYMENT_CONFIRMED 이벤트 존재 건만 조회
     */
    private BooleanBuilder confirmedFilter(
            String confirmed,
            QPayment payment,
            QPaymentEvent confirmedEvent
    ) {
        BooleanBuilder builder = new BooleanBuilder();

        if (confirmed == null || confirmed.isBlank() || CONFIRMED_ALL.equalsIgnoreCase(confirmed)) {
            return builder;
        }

        if (CONFIRMED_ONLY.equalsIgnoreCase(confirmed)) {
            builder.and(
                    JPAExpressions
                            .selectOne()
                            .from(confirmedEvent)
                            .where(
                                    confirmedEvent.paymentId.eq(payment.paymentId),
                                    confirmedEvent.eventType.eq(PaymentEventType.PAYMENT_CONFIRMED)
                            )
                            .exists()
            );
        }

        return builder;
    }

    /**
     * 기간 From 조건 생성
     * - from 당일 00:00:00 이상 조회
     */
    private BooleanBuilder createdAtGoe(MerchantPaymentSearchCondition condition, QPayment payment) {
        BooleanBuilder builder = new BooleanBuilder();

        if (condition.getFrom() != null) {
            builder.and(payment.createdAt.goe(condition.getFrom().atStartOfDay()));
        }

        return builder;
    }

    /**
     * 기간 To 조건 생성
     * - to 다음날 00:00:00 미만 조회
     * - 종료일 포함 검색 목적
     */
    private BooleanBuilder createdAtLt(MerchantPaymentSearchCondition condition, QPayment payment) {
        BooleanBuilder builder = new BooleanBuilder();

        if (condition.getTo() != null) {
            builder.and(payment.createdAt.lt(condition.getTo().plusDays(1).atStartOfDay()));
        }

        return builder;
    }

    /**
     * 키워드 검색 조건 생성
     * - paymentId / orderId / itemName 대상 부분일치 검색
     * - blank 입력 시 조건 미적용
     */
    private BooleanBuilder keywordContains(
            MerchantPaymentSearchCondition condition,
            QPayment payment,
            QOrders order
    ) {
        BooleanBuilder builder = new BooleanBuilder();

        if (condition.getKeyword() == null || condition.getKeyword().isBlank()) {
            return builder;
        }

        String keyword = condition.getKeyword().trim();

        builder.and(
                payment.paymentId.contains(keyword)
                        .or(payment.orderId.contains(keyword))
                        .or(order.itemName.contains(keyword))
        );

        return builder;
    }

    /**
     * 환불 컨텍스트 조회
     * - payment 기본 정보 조회
     * - capturedAt은 PAYMENT_CAPTURED 이벤트 occurredAt 파생
     * - refundableAmount는 capturedAmount - 승인된 refund 합계로 계산
     */
    public RefundContextResponse findRefundContext(String paymentId) {
        QPayment payment = QPayment.payment;
        QRefund refund = QRefund.refund;
        QPaymentEvent capturedAtEvent = new QPaymentEvent("capturedAtEvent");

        RefundContextResponse response = queryFactory
                .select(Projections.constructor(
                        RefundContextResponse.class,
                        payment.paymentId,
                        payment.status.stringValue(),
                        payment.capturedAmount,
                        payment.capturedAmount.subtract(
                                JPAExpressions
                                        .select(refund.amount.sum().coalesce(0L))
                                        .from(refund)
                                        .where(
                                                refund.paymentId.eq(payment.paymentId),
                                                refund.status.eq(RefundStatus.APPROVED)
                                        )
                        ),
                        payment.currency,
                        payment.merchantId,
                        JPAExpressions
                                .select(capturedAtEvent.occurredAt.min())
                                .from(capturedAtEvent)
                                .where(
                                        capturedAtEvent.paymentId.eq(payment.paymentId),
                                        capturedAtEvent.eventType.eq(PaymentEventType.PAYMENT_CAPTURED)
                                )
                ))
                .from(payment)
                .where(payment.paymentId.eq(paymentId))
                .fetchOne();

        if (response == null) {
            return null;
        }

        return response;
    }
}