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
    /**
     * A2 history SoT:
     * - OK/FAIL 은 settlement_batch.batch_key(baseDate) 기준으로 조회한다.
     * - createdAt 기간 필터는 A2 history 기준으로 사용하지 않는다.
     */
    Page<SettlementBatch> findByBatchKeyBetweenOrderByBatchKeyDesc(
            LocalDate fromInclusive,
            LocalDate toInclusive,
            Pageable pageable
    );

    Optional<SettlementBatch> findByRunId(String runId);
    Optional<SettlementBatch> findByBatchKey(LocalDate batchKey);
}