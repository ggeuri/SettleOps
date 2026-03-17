package com.settleops.domain.admin.query.trace.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AuditTraceResponseDto {
    // requestId 검색일 때만 세팅, merchantId 검색은 null
    private final String requestId ;
    private final List<AuditTraceRowDto> items;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
}
