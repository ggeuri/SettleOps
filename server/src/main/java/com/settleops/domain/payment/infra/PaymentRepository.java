package com.settleops.domain.payment.infra;

import com.settleops.domain.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {


    Optional<Payment> findByOrderId(String orderId);
}
