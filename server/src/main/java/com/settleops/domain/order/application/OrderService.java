package com.settleops.domain.order.application;

import com.settleops.domain.order.domain.Orders;
import com.settleops.domain.order.infra.OrdersRepository;
import com.settleops.global.error.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    final private OrdersRepository ordersRepository;

    @Transactional(readOnly = true)
    public Orders getByOrderId(String orderId) {

        if (orderId == null || orderId.isBlank()) {
            throw new BadRequestException("orderId는 필수입니다.");
        }

        return ordersRepository.findByOrderId(orderId)
                .orElseThrow(() ->
                        new BadRequestException("존재하지 않는 orderId 입니다.")
                );
    }

    @Transactional
    public Orders markPaid(String orderId) {
        Orders order = getByOrderId(orderId);
        order.markPaid();   // 엔티티 내부 상태 변경
        return order;       // 별도 save 필요 없음 (영속 상태라면)
    }

    @Transactional
    public Orders createSeedOrder(String merchantId, String buyerId, String itemName, Long amount) {
        Orders order = Orders.create(merchantId, buyerId, itemName, amount);
        return ordersRepository.save(order);
    }
}
