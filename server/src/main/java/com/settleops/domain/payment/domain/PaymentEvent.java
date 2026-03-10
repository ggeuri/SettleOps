package com.settleops.domain.payment.domain;

import jakarta.persistence.*;
import lombok.Getter;

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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_event_id", nullable = false)
    private Long paymentEventId;

    @Column(name = "payment_id", columnDefinition = "char(36)", nullable = false)
    private String paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 32, nullable = false)
    private PaymentEventType eventType;

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
            PaymentEventType eventType,
            PaymentStatus before,
            PaymentStatus after,
            String requestId
    ) {
        if (paymentId == null || paymentId.isBlank()) {
            throw new IllegalArgumentException("paymentId는 필수입니다.");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("eventType은 필수입니다.");
        }
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId는 필수입니다.");
        }

        PaymentEvent e = new PaymentEvent();
        e.paymentId = paymentId;
        e.eventType = eventType;
        e.statusBefore = before;
        e.statusAfter = after;
        e.requestId = requestId;
        return e;
    }

    public static PaymentEvent created(String paymentId,String requestId) {
        return of(
                paymentId,
                PaymentEventType.PAYMENT_CREATED,
                null,
                PaymentStatus.CREATED,
                requestId
        );
    }

    public static PaymentEvent captured(String paymentId, String requestId) {
        return of(
                paymentId,
                PaymentEventType.PAYMENT_CAPTURED,
                PaymentStatus.CREATED,
                PaymentStatus.CAPTURED,
                requestId
        );
    }

    // CONFIRMED는 이벤트 SoT(PAYMENT_CONFIRMED)로 고정
    public static PaymentEvent confirmed(String paymentId, String requestId) {
        return of(
                paymentId,
                PaymentEventType.PAYMENT_CONFIRMED,
                PaymentStatus.CAPTURED,
                PaymentStatus.CAPTURED,
                requestId
        );
    }

}
