package com.settleops.domain.settlement.infra;

import com.settleops.domain.settlement.entity.SettlementLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementLineRepository extends JpaRepository<SettlementLine, String> {
    List<SettlementLine> findBySettlementIdOrderByCreatedAtAsc(String settlementId);
}
