package com.settleops.domain.settlement.api;

import com.settleops.domain.settlement.application.MerchantSettlementQueryService;
import com.settleops.domain.settlement.dto.MerchantSettlementDetailResponse;
import com.settleops.domain.settlement.dto.MerchantSettlementLineItemResponse;
import com.settleops.domain.settlement.dto.MerchantSettlementListItemResponse;
import com.settleops.domain.settlement.enums.SettlementLineType;
import com.settleops.domain.settlement.enums.SettlementStatus;
import com.settleops.global.auth.MerchantSessionResolver;
import jakarta.servlet.http.HttpServletRequest;
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

public class MerchantSettlementQueryControllerTest {

    @Test
    @DisplayName("Merchant 정산 리스트 조회 시 resolver의 merchantId와 path merchantId를 서비스로 전달한다")
    void getMerchantSettlements_passesResolvedMerchantIdAndPathMerchantId() {
        MerchantSettlementQueryService merchantSettlementQueryService = Mockito.mock(MerchantSettlementQueryService.class);
        MerchantSessionResolver merchantSessionResolver = Mockito.mock(MerchantSessionResolver.class);
        MerchantSettlementQueryController controller =
                new MerchantSettlementQueryController(merchantSettlementQueryService, merchantSessionResolver);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        PageRequest pageable = PageRequest.of(0, 20);

        MerchantSettlementListItemResponse item = new MerchantSettlementListItemResponse(
                "settlement-1",
                LocalDate.of(2026, 3, 10),
                SettlementStatus.READY,
                10000L,
                0L,
                0L,
                10000L,
                LocalDateTime.of(2026, 3, 11, 10, 0)
        );

        Mockito.when(merchantSessionResolver.resolveMerchantId(request))
                .thenReturn("merchant-1");

        Mockito.when(merchantSettlementQueryService.getMerchantSettlements(
                "merchant-1",
                "merchant-1",
                PageRequest.of(0, 20)
        )).thenReturn(new PageImpl<>(List.of(item), pageable, 1));

        ResponseEntity<?> response = controller.getMerchantSettlements(
                "merchant-1",
                pageable,
                request
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();

        Mockito.verify(merchantSessionResolver).resolveMerchantId(request);
        Mockito.verify(merchantSettlementQueryService).getMerchantSettlements(
                "merchant-1",
                "merchant-1",
                PageRequest.of(0, 20)
        );
    }

    @Test
    @DisplayName("Merchant 정산 상세 조회 시 resolver의 merchantId와 settlementId를 서비스로 전달한다")
    void getMerchantSettlementDetail_passesResolvedMerchantIdAndSettlementId() {
        MerchantSettlementQueryService merchantSettlementQueryService = Mockito.mock(MerchantSettlementQueryService.class);
        MerchantSessionResolver merchantSessionResolver = Mockito.mock(MerchantSessionResolver.class);
        MerchantSettlementQueryController controller =
                new MerchantSettlementQueryController(merchantSettlementQueryService, merchantSessionResolver);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        MerchantSettlementDetailResponse detail = new MerchantSettlementDetailResponse(
                "settlement-1",
                LocalDate.of(2026, 3, 10),
                SettlementStatus.READY,
                10000L,
                0L,
                0L,
                10000L,
                List.of(
                        new MerchantSettlementLineItemResponse(
                                "1",
                                SettlementLineType.PAYMENT,
                                "payment-1",
                                10000L
                        )
                )
        );

        Mockito.when(merchantSessionResolver.resolveMerchantId(request))
                .thenReturn("merchant-1");

        Mockito.when(merchantSettlementQueryService.getMerchantSettlementDetail(
                "merchant-1",
                "merchant-1",
                "settlement-1"
        )).thenReturn(detail);

        ResponseEntity<?> response = controller.getMerchantSettlementDetail(
                "merchant-1",
                "settlement-1",
                request
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();

        Mockito.verify(merchantSessionResolver).resolveMerchantId(request);
        Mockito.verify(merchantSettlementQueryService).getMerchantSettlementDetail(
                "merchant-1",
                "merchant-1",
                "settlement-1"
        );
    }
}