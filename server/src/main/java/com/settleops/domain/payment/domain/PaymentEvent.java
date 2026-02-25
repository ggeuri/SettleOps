package com.settleops.domain.payment.domain;

import com.settleops.global.enums.Action;
import jakarta.persistence.*;
import lombok.Getter;
import org.slf4j.MDC;

import java.time.LocalDateTime;

// mysql> desc payment_event;
// +------------------+-------------+------+-----+----------------------+-------------------+
// | Field            | Type        | Null | Key | Default              | Extra             |
// +------------------+-------------+------+-----+----------------------+-------------------+
// | payment_event_id | bigint      | NO   | PRI | NULL                 | auto_increment    |
// | payment_id       | char(36)    | NO   | MUL | NULL                 |                   |
// | event_type       | varchar(32) | NO   | MUL | NULL                 |                   |
// | status_before    | varchar(64) | YES  |     | NULL                 |                   |
// | status_after     | varchar(64) | YES  |     | NULL                 |                   |
// | request_id       | char(36)    | NO   | MUL | NULL                 |                   |
// | occurred_at      | datetime(6) | NO   |     | CURRENT_TIMESTAMP(6) | DEFAULT_GENERATED |
// +------------------+-------------+------+-----+----------------------+-------------------+

@Getter
@Entity
@Table(
        name="payment_event",
        uniqueConstraints = {
                @UniqueConstraint(
                        name="uk_payment_event_payment_type",
                        columnNames = {"payment_id","event_type"}
                )
        },
        indexes = {
                @Index(name="idx_payment_event_request_id",columnList="request_id"),
                @Index(name="idx_payment_event_payment_id_occurred_at",columnList="payment_id, occurred_at"),
                @Index(name = "idx_payment_event_type_occurred_at", columnList = "event_type, occurred_at")
        }
)
public class PaymentEvent {

    private final static String MDC_KEY = "requestId";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_event_id", nullable = false)
    private Long paymentEventId;

    @Column(name = "payment_id", columnDefinition = "char(36)", nullable = false)
    private String paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 32, nullable = false)
    private Action eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_before", length = 64)
    private PaymentStatus statusBefore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_after", length = 64)
    private PaymentStatus statusAfter;

    @Column(name = "request_id", columnDefinition = "char(36)", nullable = false)
    private String requestId;

    // DB DEFAULT CURRENT_TIMESTAMP(6)
    @Column(name = "occurred_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime occurredAt;

    private static PaymentEvent of(
            String paymentId,
            Action eventType,
            PaymentStatus before,
            PaymentStatus after
    ) {
        if (paymentId == null || paymentId.isBlank()) {
            throw new IllegalArgumentException("paymentId는 필수입니다.");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("eventType은 필수입니다.");
        }

        String requestId = MDC.get(MDC_KEY);
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalStateException("requestId가 MDC에 없습니다.");
        }

        PaymentEvent e = new PaymentEvent();
        e.paymentId = paymentId;
        e.eventType = eventType;
        e.statusBefore = before;
        e.statusAfter = after;
        e.requestId = requestId;
        return e;
    }

    public static PaymentEvent created(String paymentId) {
        return of(
                paymentId,
                Action.PAYMENT_CREATED,
                null,
                PaymentStatus.CREATED
        );
    }

    public static PaymentEvent captured(String paymentId) {
        return of(
                paymentId,
                Action.PAYMENT_CAPTURED,
                PaymentStatus.CREATED,
                PaymentStatus.CAPTURED
        );
    }

}
