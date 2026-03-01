package com.settleops.domain.payment.infra;

import com.settleops.domain.payment.domain.IdempotencyRecord;
import com.settleops.domain.payment.domain.Payment;
import com.settleops.global.enums.IdempotencyTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

    /** c1-> c2 order 결제 중복 조회 */
    public Optional<IdempotencyRecord> findByTargetTypeAndTargetIdAndIdempotencyKey(
            IdempotencyTargetType targetType,
            String targetId,
            String idempotencyKey
    );
}
