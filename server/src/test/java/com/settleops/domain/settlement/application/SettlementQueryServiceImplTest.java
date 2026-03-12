package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.dto.AdminSettlementListItemResponse;
import com.settleops.domain.settlement.enums.SettlementStatus;
import com.settleops.domain.settlement.infra.SettlementRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SettlementQueryServiceImplTest {

    @Test
    @DisplayName("관리자 정산 리스트 조회 시 status 필터를 적용한다")
    void getAdminSettlements_withStatusFilter() {
        SettlementRepository settlementRepository = Mockito.mock(SettlementRepository.class);
        SettlementQueryServiceImpl service = new SettlementQueryServiceImpl(settlementRepository);

        PageRequest pageable = PageRequest.of(0, 20);

        AdminSettlementListItemResponse readyItem = new AdminSettlementListItemResponse(
                "settlement-1",
                "merchant-1",
                LocalDate.of(2026, 3, 10),
                SettlementStatus.READY,
                10000L,
                0L,
                0L,
                10000L,
                LocalDateTime.of(2026, 3, 11, 10, 0)
        );

        Page<AdminSettlementListItemResponse> page = new PageImpl<>(List.of(readyItem), pageable, 1);

        Mockito.when(settlementRepository.searchAdminSettlements(
                SettlementStatus.READY,
                null,
                pageable
        )).thenReturn(page);

        Page<AdminSettlementListItemResponse> result =
                service.getAdminSettlements(SettlementStatus.READY, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).status()).isEqualTo(SettlementStatus.READY);

        Mockito.verify(settlementRepository)
                .searchAdminSettlements(SettlementStatus.READY, null, pageable);
    }

    @Test
    @DisplayName("관리자 정산 리스트 조회 시 merchantId 필터를 적용한다")
    void getAdminSettlements_withMerchantIdFilter() {
        SettlementRepository settlementRepository = Mockito.mock(SettlementRepository.class);
        SettlementQueryServiceImpl service = new SettlementQueryServiceImpl(settlementRepository);

        PageRequest pageable = PageRequest.of(0, 20);

        AdminSettlementListItemResponse item = new AdminSettlementListItemResponse(
                "settlement-1",
                "merchant-1",
                LocalDate.of(2026, 3, 10),
                SettlementStatus.READY,
                10000L,
                0L,
                0L,
                10000L,
                LocalDateTime.of(2026, 3, 11, 10, 0)
        );

        Page<AdminSettlementListItemResponse> page = new PageImpl<>(List.of(item), pageable, 1);

        Mockito.when(settlementRepository.searchAdminSettlements(
                null,
                "merchant-1",
                pageable
        )).thenReturn(page);

        Page<AdminSettlementListItemResponse> result =
                service.getAdminSettlements(null, "merchant-1", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).merchantId()).isEqualTo("merchant-1");

        Mockito.verify(settlementRepository)
                .searchAdminSettlements(null, "merchant-1", pageable);
    }

    @Test
    @DisplayName("관리자 정산 리스트 조회 시 status와 merchantId 필터를 함께 적용한다")
    void getAdminSettlements_withStatusAndMerchantIdFilter() {
        SettlementRepository settlementRepository = Mockito.mock(SettlementRepository.class);
        SettlementQueryServiceImpl service = new SettlementQueryServiceImpl(settlementRepository);

        PageRequest pageable = PageRequest.of(0, 20);

        AdminSettlementListItemResponse item = new AdminSettlementListItemResponse(
                "settlement-1",
                "merchant-1",
                LocalDate.of(2026, 3, 10),
                SettlementStatus.READY,
                10000L,
                0L,
                0L,
                10000L,
                LocalDateTime.of(2026, 3, 11, 10, 0)
        );

        Page<AdminSettlementListItemResponse> page = new PageImpl<>(List.of(item), pageable, 1);

        Mockito.when(settlementRepository.searchAdminSettlements(
                SettlementStatus.READY,
                "merchant-1",
                pageable
        )).thenReturn(page);

        Page<AdminSettlementListItemResponse> result =
                service.getAdminSettlements(SettlementStatus.READY, "merchant-1", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).status()).isEqualTo(SettlementStatus.READY);
        assertThat(result.getContent().get(0).merchantId()).isEqualTo("merchant-1");

        Mockito.verify(settlementRepository)
                .searchAdminSettlements(SettlementStatus.READY, "merchant-1", pageable);
    }
}
