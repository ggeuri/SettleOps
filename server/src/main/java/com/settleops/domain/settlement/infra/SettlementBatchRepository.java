package com.settleops.domain.settlement.infra;

import com.settleops.domain.settlement.entity.SettlementBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface SettlementBatchRepository extends JpaRepository<SettlementBatch, Long> {
    Optional<SettlementBatch> findByBatchKey(LocalDate batchKey);
    Optional<SettlementBatch> findByRunId(String runId);
}