package com.settleops.domain.order.application;

import com.settleops.domain.order.domain.Orders;
import com.settleops.domain.payment.application.PayEventWriter;
import com.settleops.domain.payment.application.PayPaymentWriter;
import com.settleops.domain.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ConsumerOrderFacade {

    private final OrderService orderService;
    private final PayPaymentWriter payPaymentWriter;
    private final PayEventWriter payEventWriter;

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