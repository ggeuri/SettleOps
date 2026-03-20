package com.settleops.domain.hold.application;

import com.settleops.domain.hold.domain.HoldReasonCode;

public record HoldCreateCommand(
        String requestId,
        String actorId,
        String settlementId,
        HoldReasonCode reasonCode,
        String comment
) {}