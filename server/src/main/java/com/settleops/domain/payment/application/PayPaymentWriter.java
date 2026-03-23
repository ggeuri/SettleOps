package com.settleops.domain.payment.application;

import com.settleops.domain.payment.domain.Payment;
import com.settleops.domain.payment.infra.PaymentRepository;
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
public class PayPaymentWriter {

    private final PaymentRepository paymentRepository;

    /**
     * payment 생성.
     *
     * <p>현재 구조에서 payment는 order 생성 시점에 선생성된다.</p>
     * <p>orderId UNIQUE 충돌은 동일 주문에 대한 중복 생성 또는 동시 처리 경합으로 간주하고
     * 409 IN_PROGRESS를 반환한다.</p>
     */
    public Payment create(Payment payment, String orderId) {
        try {
            return paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException e) {

            if (!DbConstraintUtils.isDuplicateKey(e)) {
                throw e;
            }

            // write 시점 duplicate는 "이미 성공한 멱등 재시도"가 아니라 "동시 처리 경합"으로 본다.
            // 같은 트랜잭션 안에서 duplicate catch 후 재조회/정상 반환을 시도하면
            // rollback-only 상태로 이어질 수 있으므로 수습하지 않고 409(IN_PROGRESS)로 끊는다.
            log.info("PAYMENT 생성 충돌. orderId={}", orderId);
            throw new ConflictException(ReasonCode.IN_PROGRESS, "결제가 처리 중입니다. 잠시 후 다시 시도해주세요.");
        }
    }

    /**
     * CAPTURE 수행 후 payment 상태를 저장한다.
     */
    public Payment saveCapturedState(Payment payment) {
        return paymentRepository.saveAndFlush(payment);
    }
}