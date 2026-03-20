package com.settleops.domain.admin.query.trace.service;

import com.settleops.domain.admin.query.trace.dto.AuditTraceSearchRequestDto;
import com.settleops.global.audit.AuditLog;
import com.settleops.global.audit.AuditTraceQueryService;
import com.settleops.global.error.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditTraceServiceTest {

    @Mock
    AuditTraceQueryService auditTraceQueryService;

    @InjectMocks
    AuditTraceService auditTraceService;

    @Test
    void merchantId_only_without_from_to_should_use_last_7_days_range() {
        // Given: merchantId만 있고 from/to는 null
        AuditTraceSearchRequestDto rq = new AuditTraceSearchRequestDto(
                null,   // requestId
                "M1",   // merchantId
                null,   // entityType
                null,   // from
                null    // to
        );

        var pageable = PageRequest.of(0, 20);

        // queryService가 호출되면 빈 페이지 반환(서비스가 끝까지 진행되게)
        Page<AuditLog> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(auditTraceQueryService.findByMerchantInRange(eq("M1"), any(), any(), eq(pageable)))
                .thenReturn(emptyPage);

        // When
        auditTraceService.searchAuditTraces(rq, pageable);

        // Then: queryService에 전달된 from/to가 "최근 7일"로 잡혔는지 캡처 검증
        ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(auditTraceQueryService, times(1))
                .findByMerchantInRange(eq("M1"), fromCaptor.capture(), toCaptor.capture(), eq(pageable));

        LocalDateTime from = fromCaptor.getValue();
        LocalDateTime to = toCaptor.getValue();

        assertNotNull(from);
        assertNotNull(to);
        assertTrue(to.isAfter(from));

        long minutes = Duration.between(from, to).toMinutes();
        // 7일 = 10080분.
        assertTrue(minutes >= 10075 && minutes <= 10085);
    }

    //    merchantId 검색에서 from만 있거나 to만 있으면 400
    @Test
    void merchantId_with_only_from_should_throw_400() {
        // Given: merchantId만 있고 from || to는 null
        AuditTraceSearchRequestDto rq = new AuditTraceSearchRequestDto(
                null,                       // requestId
                "M1",                       // merchantId
                null,                       // entityType
                LocalDateTime.parse("2026-02-01T00:00:00"), // from
                null // to
        );

        var pageable = PageRequest.of(0, 20);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> auditTraceService.searchAuditTraces(rq, pageable));

        assertEquals("merchantId 검색은 from/to를 함께 보내야 합니다.", ex.getMessage());


    }

    @Test
    void merchantId_with_only_to_should_throw_400() {
        AuditTraceSearchRequestDto rq = new AuditTraceSearchRequestDto(
                null,                       // requestId
                "M1",                       // merchantId
                null,                       // entityType
                null,                       // from
                LocalDateTime.parse("2026-02-08T00:00:00")  // to
        );

        var pageable = PageRequest.of(0, 20);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> auditTraceService.searchAuditTraces(rq, pageable));

        assertEquals("merchantId 검색은 from/to를 함께 보내야 합니다.", ex.getMessage());
    }

    @Test
    void merchantId_with_to_equal_from_should_throw_400() {
        AuditTraceSearchRequestDto rq = new AuditTraceSearchRequestDto(
                null,                       // requestId
                "M1",                       // merchantId
                null,                       // entityType
                LocalDateTime.parse("2026-02-08T00:00:00"),  // from
                LocalDateTime.parse("2026-02-08T00:00:00")  // to
        );

        var pageable = PageRequest.of(0, 20);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> auditTraceService.searchAuditTraces(rq, pageable));

        assertEquals("to는 from 이후여야 합니다.", ex.getMessage());
    }

}