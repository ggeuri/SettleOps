package com.settleops.domain.refund.application;

import com.settleops.domain.refund.application.dto.ApprovedRefundAdjustment;
import com.settleops.domain.refund.infra.RefundSettlementReadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefundSettlementAssemblerImplTest {

    private RefundSettlementReadRepository readRepository;
    private RefundSettlementAssemblerImpl assembler;

    @BeforeEach
    void setUp() {
        readRepository = mock(RefundSettlementReadRepository.class);
        assembler = new RefundSettlementAssemblerImpl(readRepository);
    }

    @Test
    void baseDate_시작시각을_cutoff로_사용해_조회한다() {
        LocalDate baseDate = LocalDate.of(2026, 3, 11);
        LocalDateTime cutoff = baseDate.atStartOfDay();

        List<ApprovedRefundAdjustment> expected = List.of(
                new ApprovedRefundAdjustment(
                        "refund-1",
                        "payment-1",
                        "merchant-1",
                        3000L,
                        LocalDateTime.of(2026, 3, 10, 21, 0)
                )
        );

        when(readRepository.findApprovedRefundsWithoutSettlementLinkBefore(cutoff))
                .thenReturn(expected);

        List<ApprovedRefundAdjustment> result =
                assembler.getApprovedRefundAdjustmentsForBaseDate(baseDate);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).refundId()).isEqualTo("refund-1");
        assertThat(result.get(0).amount()).isEqualTo(3000L);

        verify(readRepository).findApprovedRefundsWithoutSettlementLinkBefore(cutoff);
    }
    @Test
    void 조회결과가_없으면_빈리스트를_그대로_반환한다() {
        LocalDate baseDate = LocalDate.of(2026, 3, 11);
        LocalDateTime cutoff = baseDate.atStartOfDay();

        when(readRepository.findApprovedRefundsWithoutSettlementLinkBefore(cutoff))
                .thenReturn(List.of());

        List<ApprovedRefundAdjustment> result =
                assembler.getApprovedRefundAdjustmentsForBaseDate(baseDate);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(readRepository).findApprovedRefundsWithoutSettlementLinkBefore(cutoff);
    }

    @Test
    void baseDate_당일_00시_정각을_정확히_cutoff로_전달한다() {
        LocalDate baseDate = LocalDate.of(2026, 3, 11);

        when(readRepository.findApprovedRefundsWithoutSettlementLinkBefore(baseDate.atStartOfDay()))
                .thenReturn(List.of());

        assembler.getApprovedRefundAdjustmentsForBaseDate(baseDate);

        verify(readRepository).findApprovedRefundsWithoutSettlementLinkBefore(
                LocalDateTime.of(2026, 3, 11, 0, 0)
        );
        verifyNoMoreInteractions(readRepository);
    }

    @Test
    void repository가_준_approved_unlinked_refund를_가공없이_그대로_반환한다() {
        LocalDate baseDate = LocalDate.of(2026, 3, 11);

        ApprovedRefundAdjustment first = new ApprovedRefundAdjustment(
                "refund-1",
                "payment-1",
                "merchant-1",
                3000L,
                LocalDateTime.of(2026, 3, 10, 23, 59, 59)
        );
        ApprovedRefundAdjustment second = new ApprovedRefundAdjustment(
                "refund-2",
                "payment-2",
                "merchant-2",
                1500L,
                LocalDateTime.of(2026, 3, 10, 10, 0)
        );

        when(readRepository.findApprovedRefundsWithoutSettlementLinkBefore(baseDate.atStartOfDay()))
                .thenReturn(List.of(first, second));

        List<ApprovedRefundAdjustment> result =
                assembler.getApprovedRefundAdjustmentsForBaseDate(baseDate);

        assertThat(result)
                .hasSize(2)
                .containsExactly(first, second);

        verify(readRepository).findApprovedRefundsWithoutSettlementLinkBefore(baseDate.atStartOfDay());
    }
}