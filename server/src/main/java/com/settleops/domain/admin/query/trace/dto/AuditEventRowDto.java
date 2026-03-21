package com.settleops.domain.admin.query.trace.dto;

import com.settleops.global.audit.EntityType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuditEventRowDto {

    private LocalDateTime occurredAt;
    private String eventType;
    private EntityType entityType;
    private String entityId;
    private String statusBefore;
    private String statusAfter;
    private String metaJson;

    public static AuditEventRowDto of(
            LocalDateTime occurredAt,
            String eventType,
            EntityType entityType,
            String entityId,
            String statusBefore,
            String statusAfter,
            String metaJson
    ) {
        return new AuditEventRowDto(
                occurredAt,
                eventType,
                entityType,
                entityId,
                statusBefore,
                statusAfter,
                (metaJson == null || metaJson.isBlank()) ? "{}" : metaJson
        );
    }
}