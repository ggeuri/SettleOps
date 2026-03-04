package com.settleops.domain.settlement.infra;

import com.settleops.domain.settlement.entity.SettlementLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementLineRepository extends JpaRepository<SettlementLine, Long> {
}
