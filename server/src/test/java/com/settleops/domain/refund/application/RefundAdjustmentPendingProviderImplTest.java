package com.settleops.domain.refund.application;

import com.settleops.domain.refund.infra.RefundAdjustmentPendingReadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RefundAdjustmentPendingProviderImplTest {

    private RefundAdjustmentPendingReadRepository readRepository;
    private RefundAdjustmentPendingProviderImpl provider;

    @BeforeEach
    void setUp() {
        readRepository = mock(RefundAdjustmentPendingReadRepository.class);
        provider = new RefundAdjustmentPendingProviderImpl(readRepository);
    }

    @Test
    void repository_true를_그대로_반환한다() {
        when(readRepository.existsPendingApprovedRefundForSettlement("S1"))
                .thenReturn(true);

        boolean result = provider.existsPendingForSettlement("S1");

        assertThat(result).isTrue();
        verify(readRepository).existsPendingApprovedRefundForSettlement("S1");
    }

    @Test
    void repository_false를_그대로_반환한다() {
        when(readRepository.existsPendingApprovedRefundForSettlement("S1"))
                .thenReturn(false);

        boolean result = provider.existsPendingForSettlement("S1");

        assertThat(result).isFalse();
        verify(readRepository).existsPendingApprovedRefundForSettlement("S1");
    }
}