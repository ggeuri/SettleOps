package com.settleops.domain.order.infra;

import com.settleops.domain.order.api.dto.ConsumerOrderDetailResponse;
import com.settleops.domain.order.domain.Orders;
import com.settleops.domain.payment.domain.Payment;
import com.settleops.domain.payment.domain.PaymentEvent;
import com.settleops.domain.payment.infra.PaymentEventRepository;
import com.settleops.domain.payment.infra.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    @DisplayName("payment가 없는 주문도 상세 조회할 수 있다")
    void findConsumerOrderDetail_withoutPayment() {
        // given
        Orders order = Orders.create(
                "m_1001",
                "buyer_2001",
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
        assertThat(result.buyerId()).isEqualTo("buyer_2001");
        assertThat(result.itemName()).isEqualTo("아이폰 14 프로");
        assertThat(result.amount()).isEqualTo(125000L);
        assertThat(result.orderStatus()).isEqualTo("CREATED");
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
                "buyer_2001",
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

        paymentEventRepository.save(PaymentEvent.created(payment.getPaymentId(), "11111111-1111-1111-1111-111111111111"));
        paymentEventRepository.save(PaymentEvent.captured(payment.getPaymentId(), "11111111-1111-1111-1111-111111111111"));
        paymentEventRepository.save(PaymentEvent.confirmed(payment.getPaymentId(), "22222222-2222-2222-2222-222222222222"));

        // when
        ConsumerOrderDetailResponse result =
                consumerOrderQueryRepository.findConsumerOrderDetail(order.getOrderId());

        // then
        assertThat(result).isNotNull();
        assertThat(result.orderId()).isEqualTo(order.getOrderId());
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
}