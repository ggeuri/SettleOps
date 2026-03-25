package com.settleops.domain.refund.application;

import com.settleops.domain.refund.api.dto.RefundTraceEntryResponseDTO;
import com.settleops.domain.refund.infra.RefundTraceQueryRepository;
import com.settleops.global.error.BadRequestException;
import com.settleops.global.error.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefundTraceEntryQueryServiceTest {

    @Test
    @DisplayName("환불이 존재하고 최신 non-no-op traceRequestId가 있으면 Trace entry를 반환한다")
    void getRefundTraceEntry_returnsTraceRequestId() {
        RefundTraceQueryRepository repository =
                Mockito.mock(RefundTraceQueryRepository.class);

        RefundTraceEntryQueryService service =
                new RefundTraceEntryQueryService(repository);

        Mockito.when(repository.existsRefund("refund-1"))
                .thenReturn(true);
        Mockito.when(repository.findLatestNonNoOpRefundRequestId("refund-1"))
                .thenReturn("request-1");

        RefundTraceEntryResponseDTO response =
                service.getRefundTraceEntry("refund-1");

        assertThat(response.getTraceRequestId()).isEqualTo("request-1");

        Mockito.verify(repository).existsRefund("refund-1");
        Mockito.verify(repository).findLatestNonNoOpRefundRequestId("refund-1");
    }

    @Test
    @DisplayName("refundId가 null 또는 blank면 BadRequestException을 던진다")
    void getRefundTraceEntry_throwsBadRequest_whenRefundIdIsBlank() {
        RefundTraceQueryRepository repository =
                Mockito.mock(RefundTraceQueryRepository.class);

        RefundTraceEntryQueryService service =
                new RefundTraceEntryQueryService(repository);

        assertThatThrownBy(() -> service.getRefundTraceEntry(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("refundId must not be null/blank");

        assertThatThrownBy(() -> service.getRefundTraceEntry(""))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("refundId must not be null/blank");

        assertThatThrownBy(() -> service.getRefundTraceEntry("   "))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("refundId must not be null/blank");

        Mockito.verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("환불이 존재하지 않으면 refund not found NotFoundException을 던진다")
    void getRefundTraceEntry_throwsNotFound_whenRefundDoesNotExist() {
        RefundTraceQueryRepository repository =
                Mockito.mock(RefundTraceQueryRepository.class);

        RefundTraceEntryQueryService service =
                new RefundTraceEntryQueryService(repository);

        Mockito.when(repository.existsRefund("refund-1"))
                .thenReturn(false);

        assertThatThrownBy(() -> service.getRefundTraceEntry("refund-1"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("refund not found");

        Mockito.verify(repository).existsRefund("refund-1");
        Mockito.verify(repository, Mockito.never())
                .findLatestNonNoOpRefundRequestId(Mockito.anyString());
    }

    @Test
    @DisplayName("환불은 존재하지만 Trace entry용 requestId가 없으면 trace entry not found NotFoundException을 던진다")
    void getRefundTraceEntry_throwsNotFound_whenTraceEntryDoesNotExist() {
        RefundTraceQueryRepository repository =
                Mockito.mock(RefundTraceQueryRepository.class);

        RefundTraceEntryQueryService service =
                new RefundTraceEntryQueryService(repository);

        Mockito.when(repository.existsRefund("refund-1"))
                .thenReturn(true);
        Mockito.when(repository.findLatestNonNoOpRefundRequestId("refund-1"))
                .thenReturn(null);

        assertThatThrownBy(() -> service.getRefundTraceEntry("refund-1"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("trace entry not found");

        Mockito.verify(repository).existsRefund("refund-1");
        Mockito.verify(repository).findLatestNonNoOpRefundRequestId("refund-1");
    }

    @Test
    @DisplayName("requestId가 blank면 trace entry not found NotFoundException을 던진다")
    void getRefundTraceEntry_throwsNotFound_whenRequestIdIsBlank() {
        RefundTraceQueryRepository repository =
                Mockito.mock(RefundTraceQueryRepository.class);

        RefundTraceEntryQueryService service =
                new RefundTraceEntryQueryService(repository);

        Mockito.when(repository.existsRefund("refund-1"))
                .thenReturn(true);
        Mockito.when(repository.findLatestNonNoOpRefundRequestId("refund-1"))
                .thenReturn("   ");

        assertThatThrownBy(() -> service.getRefundTraceEntry("refund-1"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("trace entry not found");

        Mockito.verify(repository).existsRefund("refund-1");
        Mockito.verify(repository).findLatestNonNoOpRefundRequestId("refund-1");
    }
}