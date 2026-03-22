package com.settleops.domain.order.application;

import com.settleops.domain.order.domain.Orders;
import com.settleops.domain.payment.application.PayEventWriter;
import com.settleops.domain.payment.application.PayPaymentWriter;
import com.settleops.domain.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumer 주문 생성 유스케이스를 조합하는 facade.
 *
 * <p>orders SoT 생성 책임은 {@link OrderService}가 가진다.</p>
 * <p>본 facade는 Consumer 주문 생성 시나리오에서
 * order 생성 이후 필요한 후행 작업(payment CREATED 선생성, PAYMENT_CREATED 이벤트 적재)을
 * 하나의 유스케이스로 묶어 실행하는 오케스트레이션 계층이다.</p>
 * <p>즉, payment 도메인의 상태 전이 규칙 자체를 소유하는 서비스가 아니라,
 * 주문 생성 흐름에서 order/payment/event 정합을 맞추기 위한 조합 책임을 가진다.</p>
 */
@RequiredArgsConstructor
@Service
public class ConsumerOrderFacade {

    private final OrderService orderService;
    private final PayPaymentWriter payPaymentWriter;
    private final PayEventWriter payEventWriter;

    /**
     * Consumer 주문 생성과 payment CREATED 선생성, PAYMENT_CREATED 이벤트 적재를 함께 수행한다.
     *
     * <p>주문 생성의 SoT는 orders이며, payment는 주문 생성 시점에 CREATED 상태로 선생성된다.</p>
     * <p>이 메서드는 Consumer 주문 생성 유스케이스의 정합 보장을 위해
     * order / payment / payment_event 생성을 하나의 트랜잭션으로 묶는다.</p>
     */
    @Transactional
    public Orders createOrderWithPaymentCreated(
            String merchantId,
            String buyerId,
            String itemName,
            Long amount,
            String requestId
    ) {
        Orders order = orderService.createSeedOrder(merchantId, buyerId, itemName, amount);

        Payment payment = Payment.create(
                order.getOrderId(),
                order.getMerchantId(),
                order.getBuyerId(),
                order.getAmount()
        );

        payment = payPaymentWriter.create(payment, order.getOrderId());
        payEventWriter.saveCreated(payment.getPaymentId(), requestId);

        return order;
    }
}