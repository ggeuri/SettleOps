package com.settleops.global.audit;

import com.settleops.global.error.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// audit_log 저장을 단일 진입점으로 강제(직접 save 금지)하고, requestId 포함/필수값 검증 후 일관된 포맷으로 감사로그를 남긴다.

@Component
@RequiredArgsConstructor
public class AuditLogger {
    private final AuditLogRepository auditLogRepository;
    private static final int ACTOR_ID_MAX = 32;

    private void validateActorId(String actorId) {
        if (actorId == null) throw new BadRequestException("actorId is null");
        if (actorId.isBlank()) throw new BadRequestException("actorId is blank");
        if (actorId.length() > ACTOR_ID_MAX) throw new BadRequestException("actorId too long (max 32)");
        if (actorId.chars().anyMatch(ch -> ch <= 0x20 || ch >= 0x7F)) {
            // 0x20(space) 이하 제어문자/공백 금지 + 0x7F 이상 비ASCII 금지
            throw new BadRequestException("actorId must be ASCII without whitespace");
        }

    }

    @Transactional
    public void log(AuditLogCommand cmd) {

        if (cmd == null) throw new BadRequestException("auditLogCommand is null");

        if (cmd.getRequestId() == null || cmd.getRequestId().isBlank()) {
            throw new BadRequestException("requestId is null/blank");
        }

        if (cmd.getAction() == null) throw new BadRequestException("action is null");
        if (cmd.getActorType() == null) throw new BadRequestException("actorType is null");
        validateActorId(cmd.getActorId());

        if (cmd.getEntityType() == null) throw new BadRequestException("entityType is null");
        if (cmd.getEntityId() == null) throw new BadRequestException("entityId is null");

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
                .metaJson(cmd.getMetaJson())
                .merchantId(cmd.getMerchantId())
                .build();

        auditLogRepository.save(auditLog);
    }
}