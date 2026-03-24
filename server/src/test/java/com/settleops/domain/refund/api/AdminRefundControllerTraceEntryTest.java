package com.settleops.domain.refund.api;

import com.settleops.domain.refund.api.dto.RefundTraceEntryResponseDTO;
import com.settleops.domain.refund.application.RefundAdminQueryService;
import com.settleops.domain.refund.application.RefundAdminService;
import com.settleops.domain.refund.application.RefundTraceEntryQueryService;
import com.settleops.global.web.RequestIdResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class AdminRefundControllerTraceEntryTest {

    @Test
    @DisplayName("관리자 refund trace-entry 조회 API는 refundId 기준 최신 non-no-op traceRequestId 응답을 반환한다")
    void getRefundTraceEntry_withRefundId() {
        RefundAdminService refundAdminService = Mockito.mock(RefundAdminService.class);
        RefundAdminQueryService refundAdminQueryService = Mockito.mock(RefundAdminQueryService.class);
        RequestIdResolver requestIdResolver = Mockito.mock(RequestIdResolver.class);
        RefundTraceEntryQueryService refundTraceEntryQueryService = Mockito.mock(RefundTraceEntryQueryService.class);

        AdminRefundController controller =
                new AdminRefundController(
                        refundAdminService,
                        refundAdminQueryService,
                        requestIdResolver,
                        refundTraceEntryQueryService
                );

        RefundTraceEntryResponseDTO traceEntryResponse =
                new RefundTraceEntryResponseDTO("request-1");

        Mockito.when(refundTraceEntryQueryService.getRefundTraceEntry("refund-1"))
                .thenReturn(traceEntryResponse);

        ResponseEntity<RefundTraceEntryResponseDTO> response =
                controller.getRefundTraceEntry("refund-1");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTraceRequestId()).isEqualTo("request-1");

        Mockito.verify(refundTraceEntryQueryService)
                .getRefundTraceEntry("refund-1");
    }
}