package com.settleops.domain.settlement.infra;

import com.settleops.domain.settlement.entity.Settlement;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, String>, SettlementRepositoryCustom {

    /**
     * [FREEZE] approve-paid 동시성 정책용 DB 락
     * - lock timeout: 3s (운영 응답 지연 방지, deadlock/경합 시 빠르게 실패시켜 재시도 유도)
     * - 값 변경 시 반드시 동시성 IT와 함께 조정
     */
    String APPROVE_PAID_LOCK_TIMEOUT_MS = "3000";

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = APPROVE_PAID_LOCK_TIMEOUT_MS))
    @Query("select s from Settlement s where s.settlementId = :settlementId")
    Optional<Settlement> findByIdForUpdate(@Param("settlementId") String settlementId);
}
