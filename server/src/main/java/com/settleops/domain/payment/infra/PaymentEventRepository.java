package com.settleops.domain.payment.infra;

import com.settleops.domain.payment.domain.PaymentEvent;
import com.settleops.domain.payment.domain.PaymentEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent,Long> {
    @Query("""
        select e.occurredAt
        from PaymentEvent e
        where e.paymentId = :paymentId
          and e.eventType = :eventType
    """)
    Optional<LocalDateTime> findOccurredAtByPaymentIdAndEventType(
            @Param("paymentId") String paymentId,
            @Param("eventType") PaymentEventType eventType
    );

}
