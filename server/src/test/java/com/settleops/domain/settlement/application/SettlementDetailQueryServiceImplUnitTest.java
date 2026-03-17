package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.dto.AdminSettlementDetailBaseView;
import com.settleops.domain.settlement.dto.AdminSettlementDetailResponse;
import com.settleops.domain.settlement.dto.AdminSettlementHoldSummaryResponse;
import com.settleops.domain.settlement.infra.SettlementDetailQueryRepository;
import com.settleops.domain.settlement.enums.SettlementStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SettlementDetailQueryServiceImplUnitTest {

    @Test
    @DisplayName("A4 refundAdjustmentPending은 RefundAdjustmentPolicy 결과를 따른다")
    void getAdminSettlementDetail_refundPending_followsPolicy() {
        SettlementDetailQueryRepository repository = Mockito.mock(SettlementDetailQueryRepository.class);
        RefundAdjustmentPolicy refundAdjustmentPolicy = Mockito.mock(RefundAdjustmentPolicy.class);

        SettlementDetailQueryServiceImpl service =
                new SettlementDetailQueryServiceImpl(repository, refundAdjustmentPolicy);

        Mockito.when(repository.findSettlementBase("settlement-1"))
                .thenReturn(new AdminSettlementDetailBaseView(
                        "settlement-1",
                        "merchant-1",
                        LocalDate.of(2026, 3, 10),
                        SettlementStatus.READY,
                        10000L,
                        0L,
                        0L,
                        10000L
                ));
        Mockito.when(repository.findSettlementLines("settlement-1"))
                .thenReturn(List.of());
        Mockito.when(repository.findHoldSummary("settlement-1"))
                .thenReturn(AdminSettlementHoldSummaryResponse.empty());
        Mockito.when(repository.hasApprovedRefund("settlement-1"))
                .thenReturn(true);
        Mockito.when(refundAdjustmentPolicy.isRefundAdjustmentPending("settlement-1"))
                .thenReturn(true);

        AdminSettlementDetailResponse response =
                service.getAdminSettlementDetail("settlement-1");

        assertThat(response.refund()).isNotNull();
        assertThat(response.refund().hasApprovedRefund()).isTrue();
        assertThat(response.refund().refundAdjustmentPending()).isTrue();

        Mockito.verify(repository).hasApprovedRefund("settlement-1");
        Mockito.verify(refundAdjustmentPolicy).isRefundAdjustmentPending("settlement-1");
    }

    @Test
    @DisplayName("A4 hasApprovedRefund와 refundAdjustmentPending은 서로 독립적으로 조합된다")
    void getAdminSettlementDetail_refundSummary_combinesRepositoryAndPolicy() {
        SettlementDetailQueryRepository repository = Mockito.mock(SettlementDetailQueryRepository.class);
        RefundAdjustmentPolicy refundAdjustmentPolicy = Mockito.mock(RefundAdjustmentPolicy.class);

        SettlementDetailQueryServiceImpl service =
                new SettlementDetailQueryServiceImpl(repository, refundAdjustmentPolicy);

        Mockito.when(repository.findSettlementBase("settlement-1"))
                .thenReturn(new AdminSettlementDetailBaseView(
                        "settlement-1",
                        "merchant-1",
                        LocalDate.of(2026, 3, 10),
                        SettlementStatus.READY,
                        10000L,
                        0L,
                        0L,
                        10000L
                ));
        Mockito.when(repository.findSettlementLines("settlement-1"))
                .thenReturn(List.of());
        Mockito.when(repository.findHoldSummary("settlement-1"))
                .thenReturn(AdminSettlementHoldSummaryResponse.empty());
        Mockito.when(repository.hasApprovedRefund("settlement-1"))
                .thenReturn(true);
        Mockito.when(refundAdjustmentPolicy.isRefundAdjustmentPending("settlement-1"))
                .thenReturn(false);

        AdminSettlementDetailResponse response =
                service.getAdminSettlementDetail("settlement-1");

        assertThat(response.refund()).isNotNull();
        assertThat(response.refund().hasApprovedRefund()).isTrue();
        assertThat(response.refund().refundAdjustmentPending()).isFalse();
    }
}