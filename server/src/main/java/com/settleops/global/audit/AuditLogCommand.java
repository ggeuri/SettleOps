package com.settleops.global.audit;
// 컨트롤러/서비스가 audit_log를 직접 만지지 못하게 하고,
// AuditLogger.log(cmd) 한 방에 필요한 값이 다 들어오도록 “형태를 고정”하는 DTO. (A1에서 쓸 데이터)

import com.settleops.global.enums.Action;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuditLogCommand {
    private final String requestId;
    private final String merchantId;
    private final EntityType entityType;
    private final LocalDateTime occurredAt;
    private final ActorType actorType;
    private final String actorId;
    private final Action action;
    private final String statusBefore;
    private final String statusAfter;
    private final String entityId;
    private final String metaJson;
}
