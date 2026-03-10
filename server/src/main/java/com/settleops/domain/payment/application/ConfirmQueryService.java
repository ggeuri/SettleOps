package com.settleops.domain.payment.application;

import com.settleops.domain.payment.domain.PaymentEventType;
import com.settleops.domain.payment.infra.PaymentEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConfirmQueryService {

    private final PaymentEventRepository paymentEventRepository;

    /**
     * PAYMENT_CONFIRMED 발생 시각을 조회합니다.
     * 이미 confirm 완료된 요청의 no-op 수렴 판단에 사용합니다.
     *
     * <p>별도 read tx(REQUIRES_NEW)에서 조회하여
     * 상위 트랜잭션 상태와 분리된 최종 DB 값을 확인합니다.</p>
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Optional<LocalDateTime> findConfirmedAt(String paymentId) {
        return paymentEventRepository.findOccurredAtByPaymentIdAndEventType(
                paymentId,
                PaymentEventType.PAYMENT_CONFIRMED
        );
    }

    /**
     * PAYMENT_CONFIRMED 발생 시각을 필수 조회합니다.
     * confirmedAt 응답값은 항상 event.occurred_at 기준으로 반환합니다.
     *
     * <p>값이 없으면 내부 정합성 오류로 간주합니다.</p>
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public LocalDateTime getConfirmedAtOrThrow(String paymentId) {
        return paymentEventRepository.findOccurredAtByPaymentIdAndEventType(
                        paymentId,
                        PaymentEventType.PAYMENT_CONFIRMED
                )
                .orElseThrow(() -> new IllegalStateException(
                        "CONFIRMED 이벤트가 존재하지 않습니다. paymentId=" + paymentId
                ));
    }
}