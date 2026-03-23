package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.dto.AdminSettlementDetailBaseView;
import com.settleops.domain.settlement.dto.SettlementTraceEntryResponse;
import com.settleops.domain.settlement.enums.SettlementStatus;
import com.settleops.domain.settlement.infra.SettlementDetailQueryRepository;
import com.settleops.global.error.BadRequestException;
import com.settleops.global.error.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettlementTraceEntryQueryServiceTest {

    @Test
    @DisplayName("정산이 존재하고 최신 non-no-op requestId가 있으면 Trace entry를 반환한다")
    void getSettlementTraceEntry_returnsRequestId() {
        SettlementDetailQueryRepository repository =
                Mockito.mock(SettlementDetailQueryRepository.class);

        SettlementTraceEntryQueryService service =
                new SettlementTraceEntryQueryService(repository);

        AdminSettlementDetailBaseView baseView =
                new AdminSettlementDetailBaseView(
                        "settlement-1",
                        "merchant-1",
                        LocalDate.of(2026, 3, 10),
                        SettlementStatus.READY,
                        10000L,
                        0L,
                        0L,
                        10000L
                );

        Mockito.when(repository.findSettlementBase("settlement-1"))
                .thenReturn(baseView);
        Mockito.when(repository.findLatestNonNoOpSettlementRequestId("settlement-1"))
                .thenReturn("request-1");

        SettlementTraceEntryResponse response =
                service.getSettlementTraceEntry("settlement-1");

        assertThat(response.requestId()).isEqualTo("request-1");

        Mockito.verify(repository).findSettlementBase("settlement-1");
        Mockito.verify(repository).findLatestNonNoOpSettlementRequestId("settlement-1");
    }

    @Test
    @DisplayName("settlementId가 null 또는 blank면 BadRequestException을 던진다")
    void getSettlementTraceEntry_throwsBadRequest_whenSettlementIdIsBlank() {
        SettlementDetailQueryRepository repository =
                Mockito.mock(SettlementDetailQueryRepository.class);

        SettlementTraceEntryQueryService service =
                new SettlementTraceEntryQueryService(repository);

        assertThatThrownBy(() -> service.getSettlementTraceEntry(null))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> service.getSettlementTraceEntry(""))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> service.getSettlementTraceEntry("   "))
                .isInstanceOf(BadRequestException.class);

        Mockito.verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("정산이 존재하지 않으면 NotFoundException을 던진다")
    void getSettlementTraceEntry_throwsNotFound_whenSettlementDoesNotExist() {
        SettlementDetailQueryRepository repository =
                Mockito.mock(SettlementDetailQueryRepository.class);

        SettlementTraceEntryQueryService service =
                new SettlementTraceEntryQueryService(repository);

        Mockito.when(repository.findSettlementBase("settlement-1"))
                .thenReturn(null);

        assertThatThrownBy(() -> service.getSettlementTraceEntry("settlement-1"))
                .isInstanceOf(NotFoundException.class);

        Mockito.verify(repository).findSettlementBase("settlement-1");
        Mockito.verify(repository, Mockito.never())
                .findLatestNonNoOpSettlementRequestId(Mockito.anyString());
    }

    @Test
    @DisplayName("정산은 존재하지만 Trace entry용 requestId가 없으면 NotFoundException을 던진다")
    void getSettlementTraceEntry_throwsNotFound_whenTraceEntryDoesNotExist() {
        SettlementDetailQueryRepository repository =
                Mockito.mock(SettlementDetailQueryRepository.class);

        SettlementTraceEntryQueryService service =
                new SettlementTraceEntryQueryService(repository);

        AdminSettlementDetailBaseView baseView =
                new AdminSettlementDetailBaseView(
                        "settlement-1",
                        "merchant-1",
                        LocalDate.of(2026, 3, 10),
                        SettlementStatus.READY,
                        10000L,
                        0L,
                        0L,
                        10000L
                );

        Mockito.when(repository.findSettlementBase("settlement-1"))
                .thenReturn(baseView);
        Mockito.when(repository.findLatestNonNoOpSettlementRequestId("settlement-1"))
                .thenReturn(null);

        assertThatThrownBy(() -> service.getSettlementTraceEntry("settlement-1"))
                .isInstanceOf(NotFoundException.class);

        Mockito.verify(repository).findSettlementBase("settlement-1");
        Mockito.verify(repository).findLatestNonNoOpSettlementRequestId("settlement-1");
    }

    @Test
    @DisplayName("requestId가 blank면 Trace entry가 없는 것으로 간주하고 NotFoundException을 던진다")
    void getSettlementTraceEntry_throwsNotFound_whenRequestIdIsBlank() {
        SettlementDetailQueryRepository repository =
                Mockito.mock(SettlementDetailQueryRepository.class);

        SettlementTraceEntryQueryService service =
                new SettlementTraceEntryQueryService(repository);

        AdminSettlementDetailBaseView baseView =
                new AdminSettlementDetailBaseView(
                        "settlement-1",
                        "merchant-1",
                        LocalDate.of(2026, 3, 10),
                        SettlementStatus.READY,
                        10000L,
                        0L,
                        0L,
                        10000L
                );

        Mockito.when(repository.findSettlementBase("settlement-1"))
                .thenReturn(baseView);
        Mockito.when(repository.findLatestNonNoOpSettlementRequestId("settlement-1"))
                .thenReturn("   ");

        assertThatThrownBy(() -> service.getSettlementTraceEntry("settlement-1"))
                .isInstanceOf(NotFoundException.class);

        Mockito.verify(repository).findSettlementBase("settlement-1");
        Mockito.verify(repository).findLatestNonNoOpSettlementRequestId("settlement-1");
    }
}