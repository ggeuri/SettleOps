package com.settleops.global.audit;

import com.settleops.global.enums.Action;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id", nullable = false)
    private Long auditId;

    @Column(name = "request_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String requestId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "actor_type", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private ActorType actorType;

    @Column(name = "actor_id", nullable = false, length = 32)
    private String actorId;

    @Column(name = "action", nullable = false, length = 64)
    @Enumerated(EnumType.STRING)
    private Action action;

    @Column(name = "entity_type", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String entityId;

    @Column(name = "status_before", length = 64)
    private String statusBefore;

    @Column(name = "status_after", length = 64)
    private String statusAfter;

    @Column(name = "merchant_id", length = 32)
    private String merchantId;

    // MySQL JSON 타입. columnDefinition을 JSON으로 고정해서 ddl validate 충돌 방지.
    @Column(name="meta_json", nullable = false, columnDefinition="JSON")
    private String metaJson;

    @Builder
    private AuditLog(String requestId, LocalDateTime occurredAt, ActorType actorType, String actorId, Action action, EntityType entityType, String entityId, String statusBefore, String statusAfter, String merchantId, String metaJson) {
        this.requestId = requestId;
        this.occurredAt = occurredAt;
        this.actorType = actorType;
        this.actorId = actorId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.statusBefore = statusBefore;
        this.statusAfter = statusAfter;
        this.merchantId = merchantId;
        this.metaJson = metaJson;
    }

    //occurredAt null방지
    @PrePersist
    void prePersist() {
        if (occurredAt == null) occurredAt = LocalDateTime.now();
        if (metaJson == null || metaJson.isBlank()) metaJson = "{}";
    }

}