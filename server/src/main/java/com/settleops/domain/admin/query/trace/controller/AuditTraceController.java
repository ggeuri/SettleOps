package com.settleops.domain.admin.query.trace.controller;

import com.settleops.domain.admin.query.trace.dto.AuditTraceResponseDto;
import com.settleops.domain.admin.query.trace.dto.AuditTraceSearchRequestDto;
import com.settleops.domain.admin.query.trace.service.AuditTraceService;
import com.settleops.global.audit.EntityType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AuditTraceController {
    private final AuditTraceService auditTraceService;

    @GetMapping("/audit-logs")
    public AuditTraceResponseDto search(
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) EntityType entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  LocalDateTime to,
            @PageableDefault(size = 20) Pageable pageable
    ){

        AuditTraceSearchRequestDto rq = new AuditTraceSearchRequestDto(requestId,merchantId,entityType,from,to);

        return auditTraceService.searchAuditTraces(rq,pageable);

    }

}
