package com.settleops.domain.settlement.api;

import com.settleops.domain.settlement.application.SettlementDetailQueryService;
import com.settleops.domain.settlement.application.SettlementQueryService;
import com.settleops.domain.settlement.dto.*;
import com.settleops.domain.settlement.enums.SettlementLineType;
import com.settleops.domain.settlement.enums.SettlementStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AdminSettlementQueryControllerTest {
    @Test
    @DisplayName("관리자 정산 리스트 조회 시 merchantId 파라미터를 서비스로 전달한다")
    void getSettlements_withMerchantIdFilter() {
        SettlementQueryService settlementQueryService = Mockito.mock(SettlementQueryService.class);
        SettlementDetailQueryService settlementDetailQueryService = Mockito.mock(SettlementDetailQueryService.class);
        AdminSettlementQueryController controller =
                new AdminSettlementQueryController(settlementQueryService, settlementDetailQueryService);

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

        Mockito.when(settlementQueryService.getAdminSettlements(
                null,
                "merchant-1",
                PageRequest.of(0, 20)
        )).thenReturn(new PageImpl<>(List.of(item), pageable, 1));

        ResponseEntity<?> response = controller.getSettlements(
                null,
                "merchant-1",
                pageable
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();

        Mockito.verify(settlementQueryService).getAdminSettlements(
                null,
                "merchant-1",
                PageRequest.of(0, 20)
        );
    }

    @Test
    @DisplayName("관리자 정산 리스트 조회 시 status 파라미터를 서비스로 전달한다")
    void getSettlements_withStatusFilter() {
        SettlementQueryService settlementQueryService = Mockito.mock(SettlementQueryService.class);
        SettlementDetailQueryService settlementDetailQueryService = Mockito.mock(SettlementDetailQueryService.class);
        AdminSettlementQueryController controller =
                new AdminSettlementQueryController(settlementQueryService, settlementDetailQueryService);

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

        Mockito.when(settlementQueryService.getAdminSettlements(
                SettlementStatus.READY,
                null,
                PageRequest.of(0, 20)
        )).thenReturn(new PageImpl<>(List.of(item), pageable, 1));

        ResponseEntity<?> response = controller.getSettlements(
                SettlementStatus.READY,
                null,
                pageable
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();

        Mockito.verify(settlementQueryService).getAdminSettlements(
                SettlementStatus.READY,
                null,
                PageRequest.of(0, 20)
        );
    }

    @Test
    @DisplayName("관리자 정산 상세 조회 시 settlementId를 서비스로 전달한다")
    void getSettlementDetail_withSettlementId(){
        SettlementQueryService settlementQueryService = Mockito.mock(SettlementQueryService.class);
        SettlementDetailQueryService settlementDetailQueryService = Mockito.mock(SettlementDetailQueryService.class);
        AdminSettlementQueryController controller =
                new AdminSettlementQueryController(settlementQueryService, settlementDetailQueryService);

        AdminSettlementDetailResponse detailResponse = new AdminSettlementDetailResponse(
                "settlement-1",
                "merchant-1",
                LocalDate.of(2026, 3, 10),
                SettlementStatus.READY,
                10000L,
                0L,
                0L,
                10000L,
                List.of(
                        new AdminSettlementLineItemResponse(
                                "1",
                                SettlementLineType.PAYMENT,
                                "payment-1",
                                10000L
                        )
                ),
                AdminSettlementHoldSummaryResponse.empty(),
                AdminSettlementRefundSummaryResponse.empty()
        );

        Mockito.when(settlementDetailQueryService.getAdminSettlementDetail("settlement-1"))
                .thenReturn(detailResponse);

        ResponseEntity<?> response = controller.getSettlementDetail("settlement-1");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();

        Mockito.verify(settlementDetailQueryService)
                .getAdminSettlementDetail("settlement-1");
    }
}