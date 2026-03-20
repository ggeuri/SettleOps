package com.settleops.domain.hold.application;

public record HoldApproveCommand(
        String requestId,
        String actorId,
        String comment
) {}
