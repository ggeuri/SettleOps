package com.settleops.global.config;

import com.settleops.domain.settlement.application.NoopRefundAdjustmentPolicy;
import com.settleops.domain.settlement.application.RefundAdjustmentPolicy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RefundAdjustmentPolicyConfig {

    @Bean
    @ConditionalOnMissingBean(RefundAdjustmentPolicy.class)
    public RefundAdjustmentPolicy refundAdjustmentPolicy() {
        return new NoopRefundAdjustmentPolicy();
    }
}