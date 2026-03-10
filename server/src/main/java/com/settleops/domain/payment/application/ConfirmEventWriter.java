package com.settleops.domain.payment.application;

import com.settleops.domain.payment.domain.PaymentEvent;
import com.settleops.domain.payment.infra.PaymentEventRepository;
import com.settleops.global.db.DbConstraintUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfirmEventWriter {

    private final PaymentEventRepository paymentEventRepository;

    /**
     * PAYMENT_CONFIRMED insert 전용.
     *
     * @return true  = 이번 호출이 최초 insert 성공
     *         false = duplicate key로 이미 존재(no-op)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryInsertConfirmed(String paymentId, String requestId) {
        try {
            paymentEventRepository.saveAndFlush(PaymentEvent.confirmed(paymentId, requestId));
            return true;
        } catch (DataIntegrityViolationException e) {
            if (DbConstraintUtils.isDuplicateKey(e)) {
                return false;
            }
            throw e;
        }
    }
}