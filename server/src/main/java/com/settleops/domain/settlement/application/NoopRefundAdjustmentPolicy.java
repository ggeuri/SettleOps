package com.settleops.domain.settlement.application;

public class NoopRefundAdjustmentPolicy implements RefundAdjustmentPolicy {

    @Override
    public boolean isRefundAdjustmentPending(String settlementId) {
        return false;
    }
}