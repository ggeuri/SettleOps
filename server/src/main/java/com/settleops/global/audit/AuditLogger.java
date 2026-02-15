package com.settleops.global.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// audit_log 저장을 단일 진입점으로 강제(직접 save 금지)하고, requestId 포함/필수값 검증 후 일관된 포맷으로 감사로그를 남긴다.

@Component
@RequiredArgsConstructor
public class AuditLogger {
    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(AuditLogCommand cmd) {

        if (cmd == null) throw new IllegalArgumentException("auditLogCommand is null");

        if (cmd.getRequestId() == null || cmd.getRequestId().isBlank()) {
            throw new IllegalArgumentException("requestId is null/blank");
        }

        if (cmd.getAction() == null) throw new IllegalArgumentException("action is null");
        if (cmd.getActorType() == null) throw new IllegalArgumentException("actorType is null");
        if (cmd.getActorId() == null) throw new IllegalArgumentException("actorId is null");
        if (cmd.getEntityType() == null) throw new IllegalArgumentException("entityType is null");
        if (cmd.getEntityId() == null) throw new IllegalArgumentException("entityId is null");


        String metaJson = (cmd.getMetaJson() != null && !cmd.getMetaJson().isBlank())
                ? cmd.getMetaJson()
                : "{}";

        AuditLog auditLog = AuditLog.builder()
                .requestId(cmd.getRequestId())
                .action(cmd.getAction())
                .actorType(cmd.getActorType())
                .actorId(cmd.getActorId())
                .occurredAt(cmd.getOccurredAt())
                .entityType(cmd.getEntityType())
                .entityId(cmd.getEntityId())
                .statusBefore(cmd.getStatusBefore())
                .statusAfter(cmd.getStatusAfter())
                .metaJson(metaJson)
                .merchantId(cmd.getMerchantId())
                .build();

        auditLogRepository.save(auditLog);
    }
}