package com.settleops.domain.refund.application;

import com.settleops.domain.refund.api.dto.RefundRowDTO;
import com.settleops.domain.refund.domain.RefundStatus;
import com.settleops.domain.refund.infra.RefundReadRepository;
import com.settleops.global.error.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RefundQueryServiceImplTest {

    private final RefundReadRepository refundReadRepository = mock(RefundReadRepository.class);
    private final RefundQueryServiceImpl refundQueryService =
            new RefundQueryServiceImpl(refundReadRepository);

    @Test
    @DisplayName("U6 내 환불 목록 조회는 지원하지 않는 status면 BadRequestException을 던진다")
    void myRefunds_invalidStatus_throwsBadRequest() {
        assertThrows(
                BadRequestException.class,
                () -> refundQueryService.myRefunds(
                        "merchant-1",
                        "DONE",
                        null,
                        null,
                        null,
                        "requestedAt",
                        "desc",
                        PageRequest.of(0, 20)
                )
        );

        verifyNoInteractions(refundReadRepository);
    }

    @Test
    @DisplayName("U6 내 환불 목록 조회는 REQUESTED status를 RefundStatus로 변환해 repository에 전달한다")
    void myRefunds_requestedStatus_passesParsedEnumToRepository() {
        PageRequest pageable = PageRequest.of(0, 20);

        when(refundReadRepository.findMyRefunds(
                anyString(),
                any(),
                any(),
                any(),
                any(),
                anyString(),
                anyString(),
                any()
        )).thenReturn(Page.empty(pageable));

        refundQueryService.myRefunds(
                "merchant-1",
                "REQUESTED",
                null,
                null,
                null,
                "requestedAt",
                "desc",
                pageable
        );

        ArgumentCaptor<RefundStatus> statusCaptor = ArgumentCaptor.forClass(RefundStatus.class);

        verify(refundReadRepository).findMyRefunds(
                eq("merchant-1"),
                statusCaptor.capture(),
                isNull(),
                isNull(),
                isNull(),
                eq("requestedAt"),
                eq("desc"),
                eq(pageable)
        );

        assertThat(statusCaptor.getValue()).isEqualTo(RefundStatus.REQUESTED);
    }

    @Test
    @DisplayName("U6 내 환불 목록 조회는 ALL status를 null로 처리해 전체 조회로 전달한다")
    void myRefunds_allStatus_passesNullStatusToRepository() {
        PageRequest pageable = PageRequest.of(0, 20);

        when(refundReadRepository.findMyRefunds(
                anyString(),
                any(),
                any(),
                any(),
                any(),
                anyString(),
                anyString(),
                any()
        )).thenReturn(Page.empty(pageable));

        refundQueryService.myRefunds(
                "merchant-1",
                "ALL",
                null,
                null,
                null,
                "requestedAt",
                "desc",
                pageable
        );

        verify(refundReadRepository).findMyRefunds(
                eq("merchant-1"),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq("requestedAt"),
                eq("desc"),
                eq(pageable)
        );
    }

    @Test
    @DisplayName("U6 내 환불 목록 조회는 from/to를 하루 범위 LocalDateTime으로 변환한다")
    void myRefunds_convertsDateRangeToDateTime() {
        PageRequest pageable = PageRequest.of(0, 20);
        LocalDate from = LocalDate.of(2026, 3, 20);
        LocalDate to = LocalDate.of(2026, 3, 22);

        when(refundReadRepository.findMyRefunds(
                anyString(),
                any(),
                any(),
                any(),
                any(),
                anyString(),
                anyString(),
                any()
        )).thenReturn(Page.empty(pageable));

        refundQueryService.myRefunds(
                "merchant-1",
                "REQUESTED",
                from,
                to,
                "refund",
                "requestedAt",
                "desc",
                pageable
        );

        verify(refundReadRepository).findMyRefunds(
                eq("merchant-1"),
                eq(RefundStatus.REQUESTED),
                eq(LocalDateTime.of(2026, 3, 20, 0, 0, 0)),
                eq(LocalDateTime.of(2026, 3, 22, 23, 59, 59, 999_999_999)),
                eq("refund"),
                eq("requestedAt"),
                eq("desc"),
                eq(pageable)
        );
    }

    @Test
    @DisplayName("U6 내 환불 목록 조회는 repository 결과를 그대로 반환한다")
    void myRefunds_returnsRepositoryResult() {
        PageRequest pageable = PageRequest.of(0, 20);

        RefundRowDTO row = new RefundRowDTO(
                "refund-1",
                "payment-1",
                "merchant-1",
                1000L,
                RefundStatus.REQUESTED,
                "test reason",
                LocalDateTime.of(2026, 3, 20, 10, 0),
                null
        );

        Page<RefundRowDTO> expected =
                new PageImpl<>(List.of(row), pageable, 1);

        when(refundReadRepository.findMyRefunds(
                eq("merchant-1"),
                eq(RefundStatus.REQUESTED),
                isNull(),
                isNull(),
                isNull(),
                eq("requestedAt"),
                eq("desc"),
                eq(pageable)
        )).thenReturn(expected);

        Page<RefundRowDTO> result = refundQueryService.myRefunds(
                "merchant-1",
                "REQUESTED",
                null,
                null,
                null,
                "requestedAt",
                "desc",
                pageable
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getRefundId()).isEqualTo("refund-1");
    }
}