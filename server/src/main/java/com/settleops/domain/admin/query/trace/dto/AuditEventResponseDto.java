package com.settleops.domain.admin.query.trace.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AuditEventResponseDto {

    private final String requestId;
    private final List<AuditEventRowDto> items;
}