package com.settleops.domain.settlement.infra;

import com.settleops.domain.settlement.entity.SettlementBatch;
import com.settleops.domain.settlement.enums.SettlementBatchResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementBatchRepository extends JpaRepository<SettlementBatch, Long> {
    boolean existsByBatchIdAndResult(Long batchId, SettlementBatchResult result);
}