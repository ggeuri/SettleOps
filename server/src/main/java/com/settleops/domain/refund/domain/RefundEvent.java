package com.settleops.domain.refund.domain;

import com.settleops.global.audit.ActorType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Immutable
@Entity
@Table(name = "refund_event")
public class RefundEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_event_id", nullable = false)
    private Long refundEventId;

    @Column(name = "refund_id", nullable = false, columnDefinition = "CHAR(36)")
    private String refundId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private RefundEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_before", length = 32)
    private RefundStatus statusBefore; // null 가능

    @Enumerated(EnumType.STRING)
    @Column(name = "status_after", nullable = false, length = 32)
    private RefundStatus statusAfter;

    @Column(name = "request_id", nullable = false, columnDefinition = "CHAR(36)", updatable=false)
    private String requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 16)
    private ActorType actorType;

    @Column(name = "actor_id", nullable = false, length = 32)
    private String actorId;

    @Column(name = "occurred_at", nullable = false, columnDefinition = "DATETIME(6)", updatable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    void prePersist() {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }

}