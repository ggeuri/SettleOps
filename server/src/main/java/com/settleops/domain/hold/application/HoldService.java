package com.settleops.domain.hold.application;

public interface HoldService {
    HoldCreateResponse createHold(HoldCreateCommand command);
    HoldDecisionResponse approveHold(String holdId, HoldApproveCommand command);
    HoldDecisionResponse releaseHold(String holdId, HoldReleaseCommand command);
}