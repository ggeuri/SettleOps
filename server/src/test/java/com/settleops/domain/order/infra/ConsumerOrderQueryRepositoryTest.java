package com.settleops.domain.order.infra;

import com.settleops.domain.order.api.dto.ConsumerOrderDetailResponse;
import com.settleops.domain.order.api.dto.ConsumerOrderListItemResponse;
import com.settleops.domain.order.domain.OrderStatus;
import com.settleops.domain.order.domain.Orders;
import com.settleops.domain.payment.domain.Payment;
import com.settleops.domain.payment.domain.PaymentEvent;
import com.settleops.domain.payment.infra.PaymentEventRepository;
import com.settleops.domain.payment.infra.PaymentRepository;
import com.settleops.global.error.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test-db")
@Transactional
class ConsumerOrderQueryRepositoryTest {

    @Autowired
    private ConsumerOrderQueryRepository consumerOrderQueryRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentEventRepository paymentEventRepository;

    @BeforeEach
    void setUp() {
        paymentEventRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        ordersRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("payment가 없는 주문도 상세 조회할 수 있다")
    void findConsumerOrderDetail_withoutPayment() {
        // given
        Orders order = Orders.create(
                "m_1001",
                "buyer_detail_2001",
                "아이폰 14 프로",
                125000L
        );
        ordersRepository.save(order);

        // when
        ConsumerOrderDetailResponse result =
                consumerOrderQueryRepository.findConsumerOrderDetail(order.getOrderId());

        // then
        assertThat(result).isNotNull();
        assertThat(result.orderId()).isEqualTo(order.getOrderId());
        assertThat(result.merchantId()).isEqualTo("m_1001");
        assertThat(result.buyerId()).isEqualTo("buyer_detail_2001");
        assertThat(result.itemName()).isEqualTo("아이폰 14 프로");
        assertThat(result.amount()).isEqualTo(125000L);
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(result.paymentId()).isNull();
        assertThat(result.paymentStatus()).isNull();
        assertThat(result.capturedAt()).isNull();
        assertThat(result.confirmedAt()).isNull();
        assertThat(result.events()).isEmpty();
    }

    @Test
    @DisplayName("payment와 captured/confirmed event가 있으면 상세 조회에 반영된다")
    void findConsumerOrderDetail_withPaymentAndEvents() {
        // given
        Orders order = Orders.create(
                "m_1001",
                "buyer_detail_2002",
                "아이폰 14 프로",
                125000L
        );
        ordersRepository.save(order);

        Payment payment = Payment.create(
                order.getOrderId(),
                order.getMerchantId(),
                order.getBuyerId(),
                order.getAmount()
        );
        payment.capture();
        paymentRepository.save(payment);

        paymentEventRepository.save(PaymentEvent.created(
                payment.getPaymentId(),
                "11111111-1111-1111-1111-111111111111"
        ));
        paymentEventRepository.save(PaymentEvent.captured(
                payment.getPaymentId(),
                "11111111-1111-1111-1111-111111111111"
        ));
        paymentEventRepository.save(PaymentEvent.confirmed(
                payment.getPaymentId(),
                "22222222-2222-2222-2222-222222222222"
        ));

        // when
        ConsumerOrderDetailResponse result =
                consumerOrderQueryRepository.findConsumerOrderDetail(order.getOrderId());

        // then
        assertThat(result).isNotNull();
        assertThat(result.orderId()).isEqualTo(order.getOrderId());
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(result.paymentId()).isEqualTo(payment.getPaymentId());
        assertThat(result.paymentStatus()).isEqualTo("CAPTURED");
        assertThat(result.capturedAt()).isNotNull();
        assertThat(result.confirmedAt()).isNotNull();

        assertThat(result.events())
                .extracting(ConsumerOrderDetailResponse.PaymentEventItem::eventType)
                .containsExactly("PAYMENT_CREATED", "PAYMENT_CAPTURED", "PAYMENT_CONFIRMED");

        assertThat(result.events().get(0).statusBefore()).isNull();
        assertThat(result.events().get(0).statusAfter()).isEqualTo("CREATED");
        assertThat(result.events().get(1).statusBefore()).isEqualTo("CREATED");
        assertThat(result.events().get(1).statusAfter()).isEqualTo("CAPTURED");
        assertThat(result.events().get(2).statusBefore()).isEqualTo("CAPTURED");
        assertThat(result.events().get(2).statusAfter()).isEqualTo("CAPTURED");
    }

    @Test
    @DisplayName("존재하지 않는 orderId면 null을 반환한다")
    void findConsumerOrderDetail_notFound() {
        // when
        ConsumerOrderDetailResponse result =
                consumerOrderQueryRepository.findConsumerOrderDetail("550e8400-e29b-41d4-a716-446655449999");

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("C3 목록 조회 시 로그인 buyerId의 주문만 반환한다")
    void findConsumerOrders_onlyMine() {
        // given
        String myBuyerId = "buyer_list_onlymine";
        String otherBuyerId = "buyer_list_other";

        Orders myOrder1 = Orders.create("m_1001", myBuyerId, "아이폰 14 프로", 125000L);
        Orders myOrder2 = Orders.create("m_1001", myBuyerId, "에어팟 프로", 35000L);
        Orders otherOrder = Orders.create("m_2002", otherBuyerId, "맥북 프로", 300000L);

        ordersRepository.save(myOrder1);
        ordersRepository.save(myOrder2);
        ordersRepository.save(otherOrder);

        // when
        List<ConsumerOrderListItemResponse> results =
                consumerOrderQueryRepository.findConsumerOrders(myBuyerId, null, null, null);

        // then
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(ConsumerOrderListItemResponse::orderId)
                .containsExactlyInAnyOrder(myOrder1.getOrderId(), myOrder2.getOrderId());

        assertThat(results)
                .extracting(ConsumerOrderListItemResponse::itemName)
                .containsExactlyInAnyOrder("아이폰 14 프로", "에어팟 프로");

        assertThat(results)
                .extracting(ConsumerOrderListItemResponse::orderId)
                .doesNotContain(otherOrder.getOrderId());
    }

    @Test
    @DisplayName("C3 목록 조회 시 confirmed는 PAYMENT_CONFIRMED 이벤트 존재 여부로 계산된다")
    void findConsumerOrders_confirmedDerivedFromEvent() {
        // given
        String buyerId = "buyer_list_confirmed";

        Orders notConfirmedOrder = Orders.create("m_1001", buyerId, "아이폰 14 프로", 125000L);
        Orders confirmedOrder = Orders.create("m_1001", buyerId, "에어팟 프로", 35000L);

        ordersRepository.save(notConfirmedOrder);
        ordersRepository.save(confirmedOrder);

        Payment payment1 = Payment.create(
                notConfirmedOrder.getOrderId(),
                notConfirmedOrder.getMerchantId(),
                notConfirmedOrder.getBuyerId(),
                notConfirmedOrder.getAmount()
        );
        payment1.capture();
        paymentRepository.save(payment1);

        Payment payment2 = Payment.create(
                confirmedOrder.getOrderId(),
                confirmedOrder.getMerchantId(),
                confirmedOrder.getBuyerId(),
                confirmedOrder.getAmount()
        );
        payment2.capture();
        paymentRepository.save(payment2);

        paymentEventRepository.save(PaymentEvent.created(
                payment1.getPaymentId(),
                "11111111-1111-1111-1111-111111111111"
        ));
        paymentEventRepository.save(PaymentEvent.captured(
                payment1.getPaymentId(),
                "11111111-1111-1111-1111-111111111111"
        ));

        paymentEventRepository.save(PaymentEvent.created(
                payment2.getPaymentId(),
                "22222222-2222-2222-2222-222222222222"
        ));
        paymentEventRepository.save(PaymentEvent.captured(
                payment2.getPaymentId(),
                "22222222-2222-2222-2222-222222222222"
        ));
        paymentEventRepository.save(PaymentEvent.confirmed(
                payment2.getPaymentId(),
                "33333333-3333-3333-3333-333333333333"
        ));

        // when
        List<ConsumerOrderListItemResponse> results =
                consumerOrderQueryRepository.findConsumerOrders(buyerId, null, null, null);

        // then
        ConsumerOrderListItemResponse first = results.stream()
                .filter(it -> it.orderId().equals(notConfirmedOrder.getOrderId()))
                .findFirst()
                .orElseThrow();

        ConsumerOrderListItemResponse second = results.stream()
                .filter(it -> it.orderId().equals(confirmedOrder.getOrderId()))
                .findFirst()
                .orElseThrow();

        assertThat(first.orderStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(first.paid()).isFalse();
        assertThat(first.confirmed()).isFalse();

        assertThat(second.orderStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(second.paid()).isFalse();
        assertThat(second.confirmed()).isTrue();
    }

    @Test
    @DisplayName("C3 목록 조회 시 confirmed=CONFIRMED 이면 confirmed=true row만 반환한다")
    void findConsumerOrders_filterByConfirmedConfirmed() {
        // given
        String buyerId = "buyer_list_confirmed_filter";

        Orders notConfirmedOrder = Orders.create("m_1001", buyerId, "아이폰 14 프로", 125000L);
        Orders confirmedOrder = Orders.create("m_1001", buyerId, "에어팟 프로", 35000L);

        ordersRepository.save(notConfirmedOrder);
        ordersRepository.save(confirmedOrder);

        Payment payment1 = Payment.create(
                notConfirmedOrder.getOrderId(),
                notConfirmedOrder.getMerchantId(),
                notConfirmedOrder.getBuyerId(),
                notConfirmedOrder.getAmount()
        );
        payment1.capture();
        paymentRepository.save(payment1);

        Payment payment2 = Payment.create(
                confirmedOrder.getOrderId(),
                confirmedOrder.getMerchantId(),
                confirmedOrder.getBuyerId(),
                confirmedOrder.getAmount()
        );
        payment2.capture();
        paymentRepository.save(payment2);

        paymentEventRepository.save(PaymentEvent.captured(
                payment1.getPaymentId(),
                "11111111-1111-1111-1111-111111111111"
        ));
        paymentEventRepository.save(PaymentEvent.captured(
                payment2.getPaymentId(),
                "22222222-2222-2222-2222-222222222222"
        ));
        paymentEventRepository.save(PaymentEvent.confirmed(
                payment2.getPaymentId(),
                "33333333-3333-3333-3333-333333333333"
        ));

        // when
        List<ConsumerOrderListItemResponse> results =
                consumerOrderQueryRepository.findConsumerOrders(buyerId, null, "CONFIRMED", null);

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).orderId()).isEqualTo(confirmedOrder.getOrderId());
        assertThat(results.get(0).confirmed()).isTrue();
    }

    @Test
    @DisplayName("C3 목록 조회 시 confirmed=UNCONFIRMED 이면 confirmed=false row만 반환한다")
    void findConsumerOrders_filterByConfirmedUnconfirmed() {
        // given
        String buyerId = "buyer_list_unconfirmed_filter";

        Orders notConfirmedOrder = Orders.create("m_1001", buyerId, "아이폰 14 프로", 125000L);
        Orders confirmedOrder = Orders.create("m_1001", buyerId, "에어팟 프로", 35000L);

        ordersRepository.save(notConfirmedOrder);
        ordersRepository.save(confirmedOrder);

        Payment payment1 = Payment.create(
                notConfirmedOrder.getOrderId(),
                notConfirmedOrder.getMerchantId(),
                notConfirmedOrder.getBuyerId(),
                notConfirmedOrder.getAmount()
        );
        payment1.capture();
        paymentRepository.save(payment1);

        Payment payment2 = Payment.create(
                confirmedOrder.getOrderId(),
                confirmedOrder.getMerchantId(),
                confirmedOrder.getBuyerId(),
                confirmedOrder.getAmount()
        );
        payment2.capture();
        paymentRepository.save(payment2);

        paymentEventRepository.save(PaymentEvent.captured(
                payment1.getPaymentId(),
                "11111111-1111-1111-1111-111111111111"
        ));
        paymentEventRepository.save(PaymentEvent.captured(
                payment2.getPaymentId(),
                "22222222-2222-2222-2222-222222222222"
        ));
        paymentEventRepository.save(PaymentEvent.confirmed(
                payment2.getPaymentId(),
                "33333333-3333-3333-3333-333333333333"
        ));

        // when
        List<ConsumerOrderListItemResponse> results =
                consumerOrderQueryRepository.findConsumerOrders(buyerId, null, "UNCONFIRMED", null);

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).orderId()).isEqualTo(notConfirmedOrder.getOrderId());
        assertThat(results.get(0).confirmed()).isFalse();
    }

    @Test
    @DisplayName("C3 목록 조회 시 confirmed=ALL 이면 전체를 반환한다")
    void findConsumerOrders_filterByConfirmedAll() {
        // given
        String buyerId = "buyer_list_all_filter";

        Orders notConfirmedOrder = Orders.create("m_1001", buyerId, "아이폰 14 프로", 125000L);
        Orders confirmedOrder = Orders.create("m_1001", buyerId, "에어팟 프로", 35000L);

        ordersRepository.save(notConfirmedOrder);
        ordersRepository.save(confirmedOrder);

        Payment payment1 = Payment.create(
                notConfirmedOrder.getOrderId(),
                notConfirmedOrder.getMerchantId(),
                notConfirmedOrder.getBuyerId(),
                notConfirmedOrder.getAmount()
        );
        payment1.capture();
        paymentRepository.save(payment1);

        Payment payment2 = Payment.create(
                confirmedOrder.getOrderId(),
                confirmedOrder.getMerchantId(),
                confirmedOrder.getBuyerId(),
                confirmedOrder.getAmount()
        );
        payment2.capture();
        paymentRepository.save(payment2);

        paymentEventRepository.save(PaymentEvent.captured(
                payment1.getPaymentId(),
                "11111111-1111-1111-1111-111111111111"
        ));
        paymentEventRepository.save(PaymentEvent.captured(
                payment2.getPaymentId(),
                "22222222-2222-2222-2222-222222222222"
        ));
        paymentEventRepository.save(PaymentEvent.confirmed(
                payment2.getPaymentId(),
                "33333333-3333-3333-3333-333333333333"
        ));

        // when
        List<ConsumerOrderListItemResponse> results =
                consumerOrderQueryRepository.findConsumerOrders(buyerId, null, "ALL", null);

        // then
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("C3 목록 조회 시 잘못된 confirmed 값이면 400 예외를 던진다")
    void findConsumerOrders_filterByConfirmedInvalid() {
        // given
        String buyerId = "buyer_list_invalid_confirmed";

        Orders order = Orders.create("m_1001", buyerId, "아이폰 14 프로", 125000L);
        ordersRepository.save(order);

        // when // then
        assertThatThrownBy(() ->
                consumerOrderQueryRepository.findConsumerOrders(buyerId, null, "INVALID", null)
        ).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid confirmed filter");
    }

    @Test
    @DisplayName("C3 목록 조회 시 paid는 orders.status=PAID 기준으로 계산된다")
    void findConsumerOrders_paidDerivedFromOrderStatus() {
        // given
        String buyerId = "buyer_list_paid";

        Orders createdOrder = Orders.create("m_1001", buyerId, "아이폰 14 프로", 125000L);

        Orders paidOrder = Orders.create("m_1001", buyerId, "에어팟 프로", 35000L);
        paidOrder.markPaid();

        ordersRepository.save(createdOrder);
        ordersRepository.save(paidOrder);

        Payment createdPayment = Payment.create(
                createdOrder.getOrderId(),
                createdOrder.getMerchantId(),
                createdOrder.getBuyerId(),
                createdOrder.getAmount()
        );
        createdPayment.capture();
        paymentRepository.save(createdPayment);

        Payment paidPayment = Payment.create(
                paidOrder.getOrderId(),
                paidOrder.getMerchantId(),
                paidOrder.getBuyerId(),
                paidOrder.getAmount()
        );
        paidPayment.capture();
        paymentRepository.save(paidPayment);

        // when
        List<ConsumerOrderListItemResponse> results =
                consumerOrderQueryRepository.findConsumerOrders(buyerId, null, null, null);

        // then
        ConsumerOrderListItemResponse createdRow = results.stream()
                .filter(it -> it.orderId().equals(createdOrder.getOrderId()))
                .findFirst()
                .orElseThrow();

        ConsumerOrderListItemResponse paidRow = results.stream()
                .filter(it -> it.orderId().equals(paidOrder.getOrderId()))
                .findFirst()
                .orElseThrow();

        assertThat(createdRow.orderStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(createdRow.paid()).isFalse();

        assertThat(paidRow.orderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paidRow.paid()).isTrue();
    }

    @Test
    @DisplayName("C3 목록 조회 시 status 필터가 적용된다")
    void findConsumerOrders_filterByStatus() {
        // given
        String buyerId = "buyer_list_status";

        Orders createdOrder = Orders.create("m_1001", buyerId, "아이폰 14 프로", 125000L);

        Orders paidOrder = Orders.create("m_1001", buyerId, "에어팟 프로", 35000L);
        paidOrder.markPaid();

        ordersRepository.save(createdOrder);
        ordersRepository.save(paidOrder);

        // when
        List<ConsumerOrderListItemResponse> results =
                consumerOrderQueryRepository.findConsumerOrders(buyerId, OrderStatus.PAID, null, null);

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).orderId()).isEqualTo(paidOrder.getOrderId());
        assertThat(results.get(0).orderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(results.get(0).paid()).isTrue();
    }

    @Test
    @DisplayName("C3 목록 조회 시 keyword로 itemName 검색이 가능하다")
    void findConsumerOrders_filterByKeyword() {
        // given
        String buyerId = "buyer_list_keyword";

        Orders order1 = Orders.create("m_1001", buyerId, "아이폰 14 프로", 125000L);
        Orders order2 = Orders.create("m_1001", buyerId, "에어팟 프로", 35000L);

        ordersRepository.save(order1);
        ordersRepository.save(order2);

        // when
        List<ConsumerOrderListItemResponse> results =
                consumerOrderQueryRepository.findConsumerOrders(buyerId, null, null, "에어팟");

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).itemName()).isEqualTo("에어팟 프로");
    }

    @Test
    @DisplayName("C3 목록 조회 시 createdAt 내림차순으로 정렬된다")
    void findConsumerOrders_sortedByCreatedAtDesc() throws Exception {
        // given
        String buyerId = "buyer_list_sort";

        Orders olderOrder = Orders.create("m_1001", buyerId, "오래된 주문", 10000L);
        ordersRepository.saveAndFlush(olderOrder);

        Thread.sleep(5L);

        Orders newerOrder = Orders.create("m_1001", buyerId, "최신 주문", 20000L);
        ordersRepository.saveAndFlush(newerOrder);

        // when
        List<ConsumerOrderListItemResponse> results =
                consumerOrderQueryRepository.findConsumerOrders(buyerId, null, null, null);

        // then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).orderId()).isEqualTo(newerOrder.getOrderId());
        assertThat(results.get(1).orderId()).isEqualTo(olderOrder.getOrderId());
    }
}