package com.settleops.global.audit;

import com.settleops.global.enums.ReasonCode;
import com.settleops.global.error.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// audit_log 저장을 단일 진입점으로 강제(직접 save 금지)하고, requestId 포함/필수값 검증 후 일관된 포맷으로 감사로그를 남긴다.

@Component
@RequiredArgsConstructor
public class AuditLogger {
    private final AuditLogRepository auditLogRepository;
    private static final int ACTOR_ID_MAX = 32;

    @Transactional
    public void log(AuditLogCommand cmd) {
        saveInternal(cmd);
    }

    /** 409 실패 Audit 전용(GlobalExceptionHandler 사용) */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailureRequiresNew(AuditLogCommand cmd, ReasonCode reasonCode) {
        validateFailureReason(reasonCode);
        saveInternal(cmd);
    }

    private void saveInternal(AuditLogCommand cmd) {
        validateCommand(cmd);
        auditLogRepository.save(AuditLog.of(cmd));
    }

    private void validateCommand(AuditLogCommand cmd) {
        if (cmd == null) throw new BadRequestException("auditLogCommand is null");

        if (cmd.getRequestId() == null || cmd.getRequestId().isBlank()) {
            throw new BadRequestException("requestId is null/blank");
        }

        if (cmd.getAction() == null) throw new BadRequestException("action is null");
        if (cmd.getActorType() == null) throw new BadRequestException("actorType is null");
        validateActorId(cmd.getActorId());

        if (cmd.getEntityType() == null) throw new BadRequestException("entityType is null");
        if (cmd.getEntityId() == null || cmd.getEntityId().isBlank()) {
            throw new BadRequestException("entityId is null/blank");
        }
    }

    private void validateActorId(String actorId) {
        if (actorId == null) throw new BadRequestException("actorId is null");
        if (actorId.isBlank()) throw new BadRequestException("actorId is blank");
        if (actorId.length() > ACTOR_ID_MAX) {
            throw new BadRequestException("actorId too long (max 32)");
        }
        if (actorId.chars().anyMatch(ch -> ch <= 0x20 || ch >= 0x7F)) {
            throw new BadRequestException("actorId must be ASCII without whitespace");
        }
    }

    private void validateFailureReason(ReasonCode reasonCode) {
        if (reasonCode == null) {
            throw new BadRequestException("reasonCode is null");
        }
        if (reasonCode != ReasonCode.SAME_APPROVER_NOT_ALLOWED
                && reasonCode != ReasonCode.PAID_ALREADY) {
            throw new BadRequestException("unsupported failure audit reason");
        }
    }
}