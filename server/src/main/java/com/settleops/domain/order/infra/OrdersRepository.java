package com.settleops.domain.order.infra;

import com.settleops.domain.order.domain.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrdersRepository extends JpaRepository<Orders, String> {
    public Optional<Orders> findByOrderId(String orderId);
}
