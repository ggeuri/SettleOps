package com.settleops.domain.hold.application;

import com.settleops.domain.hold.domain.HoldStatus;

import java.time.LocalDateTime;

public record HoldCreateResponse(
        String requestId,
        String holdId,
        String settlementId,
        HoldStatus status,
        LocalDateTime createdAt
) {}
