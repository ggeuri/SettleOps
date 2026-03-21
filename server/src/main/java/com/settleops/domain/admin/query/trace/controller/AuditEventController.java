package com.settleops.domain.admin.query.trace.controller;

import com.settleops.domain.admin.query.trace.dto.AuditEventResponseDto;
import com.settleops.domain.admin.query.trace.service.AuditEventQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AuditEventController {

    private final AuditEventQueryService auditEventQueryService;

    @GetMapping("/audit-events")
    public AuditEventResponseDto search(@RequestParam String requestId) {
        return auditEventQueryService.findByRequestId(requestId);
    }
}