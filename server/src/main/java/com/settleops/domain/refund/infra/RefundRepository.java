package com.settleops.domain.refund.infra;

import com.settleops.domain.refund.domain.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, String> {
    //JPA
    Optional<Refund> findByPaymentId(String paymentId);
}
