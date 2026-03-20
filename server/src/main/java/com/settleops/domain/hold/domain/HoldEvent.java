package com.settleops.domain.hold.domain;

import com.settleops.global.audit.ActorType;
import com.settleops.global.enums.Action;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "hold_event",
        indexes = {
                @Index(name = "idx_hold_event_request_id", columnList = "request_id"),
                @Index(name = "idx_hold_event_hold_id_occurred_at", columnList = "hold_id, occurred_at"),
                @Index(name = "idx_hold_event_type", columnList = "event_type")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HoldEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hold_event_id", nullable = false)
    private Long holdEventId;

    @Column(name = "hold_id", nullable = false, columnDefinition = "CHAR(36)")
    private String holdId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private Action eventType;

    @Column(name = "request_id", nullable = false, columnDefinition = "CHAR(36)")
    private String requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 32)
    private ActorType actorType;

    @Column(name = "actor_id", nullable = false, length = 32)
    private String actorId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "meta_json", nullable = false, columnDefinition = "json")
    private String metaJson;

    public static HoldEvent of(
            String holdId,
            Action eventType,
            String requestId,
            ActorType actorType,
            String actorId,
            LocalDateTime occurredAt,
            String metaJson
    ) {
        HoldEvent event = new HoldEvent();
        event.holdId = holdId;
        event.eventType = eventType;
        event.requestId = requestId;
        event.actorType = actorType;
        event.actorId = actorId;
        event.occurredAt = occurredAt;
        event.metaJson = (metaJson == null || metaJson.isBlank()) ? "{}" : metaJson;
        return event;
    }
}

//CREATE TABLE IF NOT EXISTS hold_event (
//                                          hold_event_id BIGINT      NOT NULL AUTO_INCREMENT,
//                                          hold_id       CHAR(36)    NOT NULL COMMENT 'hold.hold_id (V99에서 FK ON)',
//    event_type    VARCHAR(32) NOT NULL,
//    request_id    CHAR(36)    NOT NULL,
//    actor_type    VARCHAR(32) NOT NULL,
//    actor_id      VARCHAR(32) NOT NULL,
//    occurred_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
//    meta_json     JSON        NOT NULL DEFAULT (JSON_OBJECT()) COMMENT 'LOCKED: 재현 품질 보호',
//
//    PRIMARY KEY (hold_event_id),
//
//    KEY idx_hold_event_request_id (request_id),
//    KEY idx_hold_event_hold_id_occurred_at (hold_id, occurred_at),
//    KEY idx_hold_event_type (event_type)