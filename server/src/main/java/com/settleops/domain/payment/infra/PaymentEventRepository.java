package com.settleops.domain.payment.infra;

import com.settleops.domain.payment.domain.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent,Long> {

}
