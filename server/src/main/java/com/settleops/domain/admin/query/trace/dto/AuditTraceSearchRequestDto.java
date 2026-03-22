package com.settleops.domain.admin.query.trace.dto;

import com.settleops.global.audit.EntityType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AuditTraceSearchRequestDto {
    private String requestId;
    private String merchantId;
    private EntityType entityType;
    private LocalDateTime from;
    private LocalDateTime to;
    private boolean includeNoOp;

}
