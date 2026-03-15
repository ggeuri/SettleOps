package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.dto.MerchantSettlementDetailResponse;
import com.settleops.domain.settlement.dto.MerchantSettlementLineItemResponse;
import com.settleops.domain.settlement.dto.MerchantSettlementListItemResponse;
import com.settleops.domain.settlement.enums.SettlementLineType;
import com.settleops.domain.settlement.enums.SettlementStatus;
import com.settleops.domain.settlement.infra.SettlementRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MerchantSettlementQueryServiceImplTest {

    @Test
    @DisplayName("Merchant 정산 리스트 조회 시 로그인 merchantId와 경로 merchantId가 다르면 403을 반환한다")
    void getMerchantSettlements_forbiddenWhenMerchantMismatch() {
        SettlementRepository settlementRepository = Mockito.mock(SettlementRepository.class);
        MerchantSettlementQueryServiceImpl service = new MerchantSettlementQueryServiceImpl(settlementRepository);

        assertThatThrownBy(() ->
                service.getMerchantSettlements(
                        "merchant-1",
                        "merchant-2",
                        PageRequest.of(0, 20)
                )
        )
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(403);
                });

        Mockito.verifyNoInteractions(settlementRepository);
    }

    @Test
    @DisplayName("Merchant 정산 리스트 조회 시 같은 merchantId면 repository 결과를 반환한다")
    void getMerchantSettlements_success() {
        SettlementRepository settlementRepository = Mockito.mock(SettlementRepository.class);
        MerchantSettlementQueryServiceImpl service = new MerchantSettlementQueryServiceImpl(settlementRepository);

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

        Page<MerchantSettlementListItemResponse> page = new PageImpl<>(List.of(item), pageable, 1);

        Mockito.when(settlementRepository.searchMerchantSettlements("merchant-1", pageable))
                .thenReturn(page);

        Page<MerchantSettlementListItemResponse> result =
                service.getMerchantSettlements("merchant-1", "merchant-1", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).settlementId()).isEqualTo("settlement-1");

        Mockito.verify(settlementRepository)
                .searchMerchantSettlements("merchant-1", pageable);
    }

    @Test
    @DisplayName("Merchant 정산 상세 조회 시 같은 merchantId면 repository 결과를 반환한다")
    void getMerchantSettlementDetail_success() {
        SettlementRepository settlementRepository = Mockito.mock(SettlementRepository.class);
        MerchantSettlementQueryServiceImpl service = new MerchantSettlementQueryServiceImpl(settlementRepository);

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

        Mockito.when(settlementRepository.findMerchantSettlementDetail("merchant-1", "settlement-1"))
                .thenReturn(detail);

        MerchantSettlementDetailResponse result =
                service.getMerchantSettlementDetail("merchant-1", "merchant-1", "settlement-1");

        assertThat(result.settlementId()).isEqualTo("settlement-1");
        assertThat(result.lines()).hasSize(1);

        Mockito.verify(settlementRepository)
                .findMerchantSettlementDetail("merchant-1", "settlement-1");
    }

    @Test
    @DisplayName("Merchant 정산 상세 조회 시 결과가 없으면 404를 반환한다")
    void getMerchantSettlementDetail_notFound() {
        SettlementRepository settlementRepository = Mockito.mock(SettlementRepository.class);
        MerchantSettlementQueryServiceImpl service = new MerchantSettlementQueryServiceImpl(settlementRepository);

        Mockito.when(settlementRepository.findMerchantSettlementDetail("merchant-1", "settlement-404"))
                .thenReturn(null);

        assertThatThrownBy(() ->
                service.getMerchantSettlementDetail("merchant-1", "merchant-1", "settlement-404")
        )
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(404);
                });

        Mockito.verify(settlementRepository)
                .findMerchantSettlementDetail("merchant-1", "settlement-404");
    }

    @Test
    @DisplayName("Merchant 정산 상세 조회 시 로그인 merchantId와 경로 merchantId가 다르면 403을 반환한다")
    void getMerchantSettlementDetail_forbiddenWhenMerchantMismatch() {
        SettlementRepository settlementRepository = Mockito.mock(SettlementRepository.class);
        MerchantSettlementQueryServiceImpl service = new MerchantSettlementQueryServiceImpl(settlementRepository);

        assertThatThrownBy(() ->
                service.getMerchantSettlementDetail(
                        "merchant-1",
                        "merchant-2",
                        "settlement-1"
                )
        )
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(403);
                });

        Mockito.verifyNoInteractions(settlementRepository);
    }
}
