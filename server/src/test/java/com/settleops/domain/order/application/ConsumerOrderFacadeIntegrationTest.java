package com.settleops.domain.order.application;

import com.settleops.domain.order.domain.Orders;
import com.settleops.domain.order.infra.OrdersRepository;
import com.settleops.domain.payment.domain.Payment;
import com.settleops.domain.payment.domain.PaymentEvent;
import com.settleops.domain.payment.domain.PaymentEventType;
import com.settleops.domain.payment.domain.PaymentStatus;
import com.settleops.domain.payment.infra.PaymentEventRepository;
import com.settleops.domain.payment.infra.PaymentRepository;
import com.settleops.global.logging.RequestIdKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test-db")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest
@Transactional
class ConsumerOrderFacadeIntegrationTest {

    @Autowired
    private ConsumerOrderFacade consumerOrderFacade;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentEventRepository paymentEventRepository;

    @Test
    @DisplayName("order 생성 시 payment CREATED 선생성과 PAYMENT_CREATED 이벤트 적재가 함께 수행되고 request_id가 기록된다")
    void createOrderWithPaymentCreated_should_create_payment_and_paymentCreated_event_with_requestId() {
        // given
        String merchantId = "merchant_1";
        String buyerId = "buyer_1";
        String itemName = "테스트 상품";
        long amount = 1000L;
        String requestId = UUID.randomUUID().toString();

        bindRequestId(requestId);

        // when
        Orders order = consumerOrderFacade.createOrderWithPaymentCreated(
                merchantId,
                buyerId,
                itemName,
                amount
        );

        // then - order 생성 확인
        Orders savedOrder = ordersRepository.findByOrderId(order.getOrderId())
                .orElseThrow();

        assertThat(savedOrder.getOrderId()).isEqualTo(order.getOrderId());
        assertThat(savedOrder.getStatus().name()).isEqualTo("CREATED");
        assertThat(savedOrder.getCurrency()).isEqualTo("KRW");

        // then - payment 선생성 확인
        Payment savedPayment = paymentRepository.findByOrderId(order.getOrderId())
                .orElseThrow();

        assertThat(savedPayment.getOrderId()).isEqualTo(order.getOrderId());
        assertThat(savedPayment.getMerchantId()).isEqualTo(merchantId);
        assertThat(savedPayment.getBuyerId()).isEqualTo(buyerId);
        assertThat(savedPayment.getRequestedAmount()).isEqualTo(amount);
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.CREATED);

        // then - PAYMENT_CREATED 이벤트 적재 및 request_id 확인
        PaymentEvent createdEvent = paymentEventRepository
                .findByPaymentIdAndEventType(savedPayment.getPaymentId(), PaymentEventType.PAYMENT_CREATED)
                .orElseThrow();

        assertThat(createdEvent.getPaymentId()).isEqualTo(savedPayment.getPaymentId());
        assertThat(createdEvent.getEventType()).isEqualTo(PaymentEventType.PAYMENT_CREATED);
        assertThat(createdEvent.getRequestId()).isEqualTo(requestId);
        assertThat(createdEvent.getStatusBefore()).isNull();
        assertThat(createdEvent.getStatusAfter()).isEqualTo(PaymentStatus.CREATED);
    }

    private void bindRequestId(String requestId) {
        MDC.put(RequestIdKeys.MDC_KEY, requestId);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }
}