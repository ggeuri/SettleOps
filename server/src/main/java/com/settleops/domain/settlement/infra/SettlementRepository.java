package com.settleops.domain.settlement.infra;

import com.settleops.domain.settlement.entity.Settlement;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Settlement s where s.settlementId = :settlementId")
    Optional<Settlement> findByIdForUpdate(@Param("settlementId") String settlementId);
}
