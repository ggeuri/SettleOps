package com.settleops.domain.settlement.infra;

import com.settleops.domain.settlement.entity.SettlementBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SettlementBatchRepository extends JpaRepository<SettlementBatch, Long> {
    Page<SettlementBatch> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime from,
            LocalDateTime toExclusive,
            Pageable pageable
    );

    // NOTE: A2 history 통합 조회에서 사용. A2 화면 확장(필터/상세) 시 재사용 가능.
    Page<SettlementBatch> findByBatchKeyBetweenOrderByBatchKeyDesc(
            LocalDate fromInclusive,
            LocalDate toInclusive,
            Pageable pageable
    );

    Optional<SettlementBatch> findByBatchKey(LocalDate batchKey);
    Optional<SettlementBatch> findByRunId(String runId);
}