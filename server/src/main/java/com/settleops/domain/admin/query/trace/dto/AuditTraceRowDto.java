package com.settleops.domain.admin.query.trace.dto;

import com.settleops.global.audit.ActorType;
import com.settleops.global.audit.AuditLog;
import com.settleops.global.audit.EntityType;
import com.settleops.global.enums.Action;
import com.settleops.global.error.BadRequestException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuditTraceRowDto {

    //    3.  DTO 매핑(A1 6컬럼 중심)
//    엔티티(AuditLog)를 그대로 반환 금지
//    응답 필드 고정: (occurredAt,actorType/actorId, action,entityType, entityId, statusBeforeAfter, metaJson 등)
    private LocalDateTime occurredAt;
    private ActorType actorType;
    private String actorId;
    private Action action;
    private EntityType entityType;
    private String entityId;
    private String statusBefore;
    private String statusAfter;
    private String metaJson;

    // 확장 필드(기본은 숨김 처리 가능)
    private Long auditId;
    private String merchantId;
    private String requestId;

    public static AuditTraceRowDto from(AuditLog log) {
        if (log == null) throw new BadRequestException("요청 값이 올바르지 않습니다.");

        return new AuditTraceRowDto(
                log.getOccurredAt(),
                log.getActorType(),
                log.getActorId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getStatusBefore(),
                log.getStatusAfter(),

                // metaJson은 DDL/PrePersist 상 NOT NULL이지만, 방어적으로 한 번 더 처리
                (log.getMetaJson() == null || log.getMetaJson().isBlank()) ? "{}" : log.getMetaJson(),
                log.getAuditId(),
                log.getMerchantId(),
                log.getRequestId()
        );
    }


}