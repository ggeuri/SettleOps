package com.settleops.domain.settlement.infra;

import com.settleops.domain.settlement.entity.SettlementBatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementBatchRepository extends JpaRepository<SettlementBatch, Long> {
}