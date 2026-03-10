package com.settleops.domain.settlement.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SettlementBatchSkipResponse(
        LocalDate baseDate,      // meta_json.baseDate
        String runId,            // meta_json.runId (기존 runId)
        String requestId,        // audit_log.request_id
        String actorId,          // audit_log.actor_id
        LocalDateTime occurredAt // audit_log.occurred_at
) {}