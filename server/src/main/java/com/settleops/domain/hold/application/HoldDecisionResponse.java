package com.settleops.domain.hold.application;

import com.settleops.domain.hold.domain.HoldStatus;

import java.time.LocalDateTime;

public record HoldDecisionResponse(
        String requestId,
        HoldStatus status,
        LocalDateTime decidedAt
) {}