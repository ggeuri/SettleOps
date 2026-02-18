package com.settleops.global.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

// AuditLog 조회(Trace/A1)를 위해 requestId·merchantId 기준 페이징 검색을 제공하는 조회 전용(Query) 인터페이스

public interface AuditLogQuery {
    Page<AuditLog> findByRequestIdOrderByOccurredAtDesc(String requestId, Pageable pageable);

    Page<AuditLog> findByRequestIdAndEntityTypeOrderByOccurredAtDesc(
            String requestId, EntityType entityType, Pageable pageable);

    Page<AuditLog> findByMerchantIdAndOccurredAtBetweenOrderByOccurredAtDesc(
            String merchantId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<AuditLog> findByMerchantIdAndEntityTypeAndOccurredAtBetweenOrderByOccurredAtDesc(
            String merchantId, EntityType entityType, LocalDateTime from, LocalDateTime to, Pageable pageable);
}