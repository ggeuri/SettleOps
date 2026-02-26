package com.settleops.domain.settlement.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@ConditionalOnMissingBean(RefundAdjustmentPolicy.class)
@Component
public class NoopRefundAdjustmentPolicy implements RefundAdjustmentPolicy {

    @Override
    public boolean isRefundAdjustmentPending(String settlementId) {
        return false;
    }
}