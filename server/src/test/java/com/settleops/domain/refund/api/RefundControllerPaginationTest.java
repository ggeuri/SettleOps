package com.settleops.domain.refund.api;

import com.settleops.domain.refund.api.dto.RefundRowDTO;
import com.settleops.domain.refund.application.RefundCommandService;
import com.settleops.domain.refund.application.RefundQueryService;
import com.settleops.global.web.RequestIdResolver;
import com.settleops.global.web.pagination.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;

class RefundControllerPaginationTest {

    @Test
    @DisplayName("U6 내 환불 목록 조회는 PageResponse 구조를 반환한다")
    void myRefunds_returnsPageResponse() {
        RefundCommandService refundCommandService = Mockito.mock(RefundCommandService.class);
        RefundQueryService refundQueryService = Mockito.mock(RefundQueryService.class);
        RequestIdResolver requestIdResolver = Mockito.mock(RequestIdResolver.class);

        RefundController controller =
                new RefundController(refundCommandService, refundQueryService, requestIdResolver);

        RefundRowDTO item = new RefundRowDTO(
                "refund-1",
                "payment-1",
                "merchant-1",
                1000L,
                null,
                "test reason",
                LocalDateTime.of(2026, 3, 20, 10, 0),
                null
        );

        Page<RefundRowDTO> resultPage =
                new PageImpl<>(
                        List.of(item),
                        PageRequest.of(0, 20),
                        1
                );

        Mockito.when(refundQueryService.myRefunds(
                eq("merchant-1"),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq("requestedAt"),
                eq("desc"),
                any()
        )).thenReturn(resultPage);

        ResponseEntity<PageResponse<RefundRowDTO>> response =
                controller.myRefunds(
                        null,
                        null,
                        null,
                        null,
                        "requestedAt",
                        "desc",
                        0,
                        20,
                        "merchant-1"
                );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();

        PageResponse<RefundRowDTO> body = response.getBody();
        assertThat(body.items()).hasSize(1);
        assertThat(body.page()).isEqualTo(0);
        assertThat(body.size()).isEqualTo(20);
        assertThat(body.totalElements()).isEqualTo(1);
        assertThat(body.totalPages()).isEqualTo(1);
        assertThat(body.hasNext()).isFalse();
        assertThat(body.hasPrevious()).isFalse();
    }
}