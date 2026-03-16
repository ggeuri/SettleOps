package com.settleops.domain.hold.infra;

import com.settleops.domain.hold.entity.Hold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HoldRepository extends JpaRepository<Hold, String> {
    Optional<Hold> findBySettlementId(String settlementId);
    boolean existsBySettlementId(String settlementId);
}
