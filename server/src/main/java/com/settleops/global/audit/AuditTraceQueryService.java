package com.settleops.global.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditTraceQueryService {
    private final AuditLogRepository auditLogRepository;

    public Page<AuditLog> findByRequestId(String requestId, Pageable pageable){
      return auditLogRepository.findByRequestIdOrderByOccurredAtDesc(requestId,pageable) ;
    }

    public Page<AuditLog> findByRequestIdAndEntityType(String requestId, EntityType entityType, Pageable pageable){
        return auditLogRepository.findByRequestIdAndEntityTypeOrderByOccurredAtDesc(requestId,entityType,pageable);
    }
    public Page<AuditLog> findByMerchantInRange(String merchantId, LocalDateTime from, LocalDateTime to, Pageable pageable){
        return auditLogRepository.findByMerchantIdAndOccurredAtBetweenOrderByOccurredAtDesc(merchantId,from,to,pageable);
    }
    public Page<AuditLog> findByMerchantInRangeAndEntityType(String merchantId, EntityType entityType, LocalDateTime from, LocalDateTime to, Pageable pageable){
        return auditLogRepository.findByMerchantIdAndEntityTypeAndOccurredAtBetweenOrderByOccurredAtDesc(merchantId,entityType,from,to,pageable);
    }

}
