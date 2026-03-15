package com.settleops.domain.settlement.infra;

import com.settleops.domain.settlement.entity.Hold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HoldRepository extends JpaRepository<Hold, String> {
    Optional<Hold> findBySettlementId(String settlementId);
}
