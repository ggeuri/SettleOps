package com.settleops.domain.hold.application;

public record HoldReleaseCommand (
            String requestId,
            String actorId,
            String comment
    ) {}