package com.settleops.domain.refund.api;

import com.settleops.domain.refund.api.dto.AdminRefundListItemDTO;
import com.settleops.domain.refund.application.RefundAdminQueryService;
import com.settleops.domain.refund.application.RefundAdminService;
import com.settleops.domain.refund.domain.RefundStatus;
import com.settleops.global.web.RequestIdResolver;
import com.settleops.global.web.pagination.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdminRefundControllerPaginationTest {

    @Test
    @DisplayName("환불 큐 조회는 PageResponse 형태로 반환한다")
    void list_returnsPageResponse() {
        RefundAdminService refundAdminService = Mockito.mock(RefundAdminService.class);
        RefundAdminQueryService refundAdminQueryService = Mockito.mock(RefundAdminQueryService.class);
        RequestIdResolver requestIdResolver = Mockito.mock(RequestIdResolver.class);

        AdminRefundController controller =
                new AdminRefundController(refundAdminService, refundAdminQueryService, requestIdResolver);

        AdminRefundListItemDTO item = new AdminRefundListItemDTO(
                "refund-1",
                "payment-1",
                "merchant-1",
                1000L,
                RefundStatus.REQUESTED,
                LocalDateTime.of(2026, 3, 20, 10, 0),
                null
        );

        PageRequest pageable = PageRequest.of(0, 20);

        Mockito.when(refundAdminQueryService.list(null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(item), pageable, 1));

        ResponseEntity<PageResponse<AdminRefundListItemDTO>> response =
                controller.list(null, null, null, 0, 20);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();

        PageResponse<AdminRefundListItemDTO> body = response.getBody();
        assertThat(body.items()).hasSize(1);
        assertThat(body.page()).isEqualTo(0);
        assertThat(body.size()).isEqualTo(20);
        assertThat(body.totalElements()).isEqualTo(1);
        assertThat(body.totalPages()).isEqualTo(1);
        assertThat(body.hasNext()).isFalse();
        assertThat(body.hasPrevious()).isFalse();
    }
}