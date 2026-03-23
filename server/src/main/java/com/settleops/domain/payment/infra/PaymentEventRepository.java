package com.settleops.domain.payment.infra;

import com.settleops.domain.payment.domain.PaymentEvent;
import com.settleops.domain.payment.domain.PaymentEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent,Long> {

    Optional<PaymentEvent> findByPaymentIdAndEventType(String paymentId, PaymentEventType eventType);

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

    // ===================== A2: CONFIRMED 대상 조회 (LOCKED) =====================

    interface ConfirmedPaymentRow {
        String getPaymentId();
        String getMerchantId();
        long getCapturedAmount();
    }

    /**
     * LOCKED: CONFIRMED는 payment.status가 아니라 payment_event(PAYMENT_CONFIRMED)로만 판정
     * baseDate(KST) = occurred_at DATE 기준 필터
     * - H2(MySQL mode)에서도 date() 동작하도록 native query 사용
     */
    @Query(value = """
    select
        p.payment_id        as paymentId,
        p.merchant_id       as merchantId,
        p.captured_amount   as capturedAmount
    from payment_event e
    join payment p on p.payment_id = e.payment_id
    where e.event_type = 'PAYMENT_CONFIRMED'
      and e.occurred_at >= :from
      and e.occurred_at <  :to
    """, nativeQuery = true)
    List<ConfirmedPaymentRow> findConfirmedPaymentsByOccurredAtRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    long countByPaymentIdAndEventType(String paymentId, PaymentEventType eventType);
}
