package com.settleops.domain.admin.query.trace.service;

import com.settleops.domain.admin.query.trace.dto.AuditTraceResponseDto;
import com.settleops.domain.admin.query.trace.dto.AuditTraceRowDto;
import com.settleops.domain.admin.query.trace.dto.AuditTraceSearchRequestDto;
import com.settleops.global.audit.AuditLog;
import com.settleops.global.audit.AuditTraceQueryService;
import com.settleops.global.audit.EntityType;
import com.settleops.global.error.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditTraceService {
    private final AuditTraceQueryService auditTraceQueryService;

    public AuditTraceResponseDto searchAuditTraces(AuditTraceSearchRequestDto rq, Pageable pageable) {
        if (rq == null) throw new BadRequestException("요청 값이 올바르지 않습니다.");

        String requestId = rq.getRequestId();
        String merchantId = rq.getMerchantId();
        EntityType entityType = rq.getEntityType();
        LocalDateTime from = rq.getFrom();
        LocalDateTime to = rq.getTo();
        LocalDateTime now = LocalDateTime.now();

        boolean hasRequestId = requestId != null && !requestId.isBlank();
        boolean hasMerchantId = merchantId != null && !merchantId.isBlank();
        boolean hasEntityType = entityType != null;

        if (!hasRequestId && !hasMerchantId) {
            throw new BadRequestException("requestId 또는 merchantId는 필수입니다.");
        }

        Page<AuditLog> pageResult = null;

        if (hasRequestId) {
            if (hasEntityType) {
                pageResult = auditTraceQueryService.findByRequestIdAndEntityType(requestId, entityType, pageable);
            } else {
                pageResult = auditTraceQueryService.findByRequestId(requestId, pageable);
            }
        } else if (hasMerchantId) {
            if (to == null && from == null) {
                to = now;
                from = to.minusDays(7);
            }
            if (from == null || to == null)
                throw new BadRequestException("merchantId 검색은 from/to를 함께 보내야 합니다.");
            if (!to.isAfter(from)) throw new BadRequestException("to는 from 이후여야 합니다.");

            if (hasEntityType) {
                pageResult = auditTraceQueryService.findByMerchantInRangeAndEntityType(merchantId, entityType, from, to, pageable);
            } else {
                pageResult = auditTraceQueryService.findByMerchantInRange(merchantId, from, to, pageable);
            }
        }
        if (pageResult == null) throw new IllegalStateException("pageResult is null");

        int page = pageResult.getNumber();
        int size = pageResult.getSize();
        long totalElements = pageResult.getTotalElements();
        int totalPages = pageResult.getTotalPages();

        List<AuditTraceRowDto> items = pageResult.getContent().stream().map(AuditTraceRowDto::from).toList();

        return new AuditTraceResponseDto(
                hasRequestId ? requestId : null,
                items,
                page,
                size,
                totalElements,
                totalPages
        );
    }
}
