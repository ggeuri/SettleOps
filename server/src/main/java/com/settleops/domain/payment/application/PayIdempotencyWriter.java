package com.settleops.domain.payment.application;

import com.settleops.domain.payment.domain.IdempotencyRecord;
import com.settleops.domain.payment.infra.IdempotencyRecordRepository;
import com.settleops.global.db.DbConstraintUtils;
import com.settleops.global.enums.IdempotencyTargetType;
import com.settleops.global.enums.ReasonCode;
import com.settleops.global.error.ConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayIdempotencyWriter {

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    /**
     * 성공 결과를 idempotency_record에 저장한다.
     *
     * <p>동일 키 중복 insert는 동시 처리 중으로 간주한다.</p>
     */
    public void saveSuccess(
            IdempotencyTargetType targetType,
            String orderId,
            String idempotencyKey,
            String paymentId,
            String requestId
    ) {
        try {
            idempotencyRecordRepository.saveAndFlush(
                    IdempotencyRecord.success(
                            targetType,
                            orderId,
                            idempotencyKey,
                            paymentId,
                            200,
                            requestId
                    )
            );
        } catch (DataIntegrityViolationException e) {
            if (!DbConstraintUtils.isDuplicateKey(e)) {
                throw e;
            }

            // duplicate는 동시 처리 경합으로 간주하고 상위 정책에 따라 예외 처리한다.
            log.info("IDEMPOTENCY_RECORD 저장 충돌. orderId={}, idempotencyKey={}", orderId, idempotencyKey);
            throw new ConflictException(ReasonCode.IN_PROGRESS, "결제가 처리 중입니다. 잠시 후 다시 시도해주세요.");
        }
    }
}