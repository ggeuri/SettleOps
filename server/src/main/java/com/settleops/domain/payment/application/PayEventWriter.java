package com.settleops.domain.payment.application;

import com.settleops.domain.payment.domain.PaymentEvent;
import com.settleops.domain.payment.infra.PaymentEventRepository;
import com.settleops.global.db.DbConstraintUtils;
import com.settleops.global.enums.ReasonCode;
import com.settleops.global.error.ConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayEventWriter {

    private final PaymentEventRepository paymentEventRepository;

    /**
     * PAYMENT_CREATED 이벤트 저장.
     *
     * <p>duplicate는 동시 처리 중으로 간주한다.</p>
     */
    public void saveCreated(String paymentId, String requestId) {
        try {
            paymentEventRepository.saveAndFlush(
                    PaymentEvent.created(paymentId, requestId)
            );
        } catch (DataIntegrityViolationException e) {
            if (!DbConstraintUtils.isDuplicateKey(e)) {
                throw e;
            }

            log.info("PAYMENT_CREATED 이벤트 충돌. paymentId={}", paymentId);
            throw new ConflictException(ReasonCode.IN_PROGRESS, "결제가 처리 중입니다. 잠시 후 다시 시도해주세요.");
        }
    }

    /**
     * PAYMENT_CAPTURED 이벤트 저장.
     *
     * <p>duplicate는 동시 처리 중으로 간주한다.</p>
     */
    public void saveCaptured(String paymentId, String requestId) {
        try {
            paymentEventRepository.saveAndFlush(
                    PaymentEvent.captured(paymentId, requestId)
            );
        } catch (DataIntegrityViolationException e) {
            if (!DbConstraintUtils.isDuplicateKey(e)) {
                throw e;
            }
            // duplicate는 동시 처리 경합으로 간주하고 상위 정책에 따라 예외 처리한다.
            log.info("PAYMENT_CAPTURED 이벤트 충돌. paymentId={}", paymentId);
            throw new ConflictException(ReasonCode.IN_PROGRESS, "결제가 처리 중입니다. 잠시 후 다시 시도해주세요.");
        }
    }
}