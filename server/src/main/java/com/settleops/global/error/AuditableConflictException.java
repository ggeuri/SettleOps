package com.settleops.global.error;

import com.settleops.global.audit.ActorType;
import com.settleops.global.audit.EntityType;
import com.settleops.global.enums.Action;
import com.settleops.global.enums.ReasonCode;
import lombok.Getter;

@Getter
public class AuditableConflictException extends ConflictException {

    private final ActorType actorType;
    private final String actorId;
    private final Action action;
    private final EntityType entityType;
    private final String entityId;
    private final String merchantId;
    private final String statusBefore;
    private final String statusAfter;
    private final String comment;

    public AuditableConflictException(
            ReasonCode reasonCode,
            String message,
            ActorType actorType,
            String actorId,
            Action action,
            EntityType entityType,
            String entityId,
            String merchantId,
            String statusBefore,
            String statusAfter,
            String comment
    ) {
        super(reasonCode, message);
        this.actorType = actorType;
        this.actorId = actorId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.merchantId = merchantId;
        this.statusBefore = statusBefore;
        this.statusAfter = statusAfter;
        this.comment = comment;
    }
}