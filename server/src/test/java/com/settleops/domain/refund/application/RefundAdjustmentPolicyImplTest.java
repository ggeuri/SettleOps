package com.settleops.domain.refund.application;

import com.settleops.global.error.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RefundAdjustmentPolicyImplTest {

    private RefundAdjustmentPendingProvider pendingProvider;
    private RefundAdjustmentPolicyImpl policy;

    @BeforeEach
    void setUp() {
        pendingProvider = mock(RefundAdjustmentPendingProvider.class);
        policy = new RefundAdjustmentPolicyImpl(pendingProvider);
    }

    @Test
    void settlementId가_blank면_400() {
        assertThatThrownBy(() -> policy.isRefundAdjustmentPending(" "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("settlementId");
    }

    @Test
    void 미반영_승인환불이_있으면_true() {
        when(pendingProvider.existsPendingForSettlement("S1"))
                .thenReturn(true);

        boolean result = policy.isRefundAdjustmentPending("S1");

        assertThat(result).isTrue();
        verify(pendingProvider).existsPendingForSettlement("S1");
    }

    @Test
    void 미반영_승인환불이_없으면_false() {
        when(pendingProvider.existsPendingForSettlement("S1"))
                .thenReturn(false);

        boolean result = policy.isRefundAdjustmentPending("S1");

        assertThat(result).isFalse();
        verify(pendingProvider).existsPendingForSettlement("S1");
    }
}