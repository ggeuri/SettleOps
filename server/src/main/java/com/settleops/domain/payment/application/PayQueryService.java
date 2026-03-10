package com.settleops.domain.payment.application;

import com.settleops.domain.payment.domain.IdempotencyRecord;
import com.settleops.domain.payment.domain.Payment;
import com.settleops.domain.payment.domain.PaymentEventType;
import com.settleops.domain.payment.domain.PaymentStatus;
import com.settleops.domain.payment.infra.IdempotencyRecordRepository;
import com.settleops.domain.payment.infra.PaymentEventRepository;
import com.settleops.domain.payment.infra.PaymentRepository;
import com.settleops.global.enums.IdempotencyTargetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayQueryService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;

    /**
     * 멱등키 기준으로 "성공 완료된" 기존 결제를 조회한다.
     *
     * <p>idempotency_record가 없으면 Optional.empty()를 반환한다.</p>
     * <p>idempotency_record는 존재하지만 아직 성공 결과가 완성되지 않았다면
     * (paymentId / responseStatus 미기록) Optional.empty()를 반환한다.</p>
     * <p>성공 완료 row인데 참조 payment가 없으면 정합성 오류로 간주한다.</p>
     */
    public Optional<Payment> findSucceededIdempotentPayment(
            IdempotencyTargetType targetType,
            String targetId,
            String idempotencyKey
    ) {
        Optional<IdempotencyRecord> recordOpt = idempotencyRecordRepository
                .findByTargetTypeAndTargetIdAndIdempotencyKey(targetType, targetId, idempotencyKey);

        if (recordOpt.isEmpty()) {
            return Optional.empty();
        }

        IdempotencyRecord record = recordOpt.get();

        // 선점 row만 존재하고 아직 성공 결과가 완성되지 않은 상태
        if (record.getPaymentId() == null || record.getResponseStatus() == null) {
            return Optional.empty();
        }

        return Optional.of(
                paymentRepository.findById(record.getPaymentId())
                        .orElseThrow(() -> new IllegalStateException(
                                "idempotency_record는 성공 완료 상태지만 참조 payment가 없습니다. targetId=" + targetId
                        ))
        );
    }

    /**
     * 멱등키 기준으로 선점된 row 자체를 조회한다.
     *
     * <p>PayService에서 owner 선점 후 성공 마킹 시 사용한다.</p>
     */
    public IdempotencyRecord getIdempotencyRecordOrThrow(
            IdempotencyTargetType targetType,
            String targetId,
            String idempotencyKey
    ) {
        return idempotencyRecordRepository
                .findByTargetTypeAndTargetIdAndIdempotencyKey(targetType, targetId, idempotencyKey)
                .orElseThrow(() -> new IllegalStateException(
                        "idempotency_record가 존재하지 않습니다. targetId=" + targetId
                ));
    }

    /**
     * orderId 기준 payment를 조회한다.
     */
    public Optional<Payment> findPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    /**
     * PAYMENT_CAPTURED 이벤트의 occurred_at을 조회한다.
     *
     * <p>capturedAt의 SoT는 payment.updatedAt이 아니라
     * PAYMENT_CAPTURED 이벤트의 occurred_at이다.</p>
     */
    public LocalDateTime getCapturedAtOrThrow(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalStateException(
                        "payment가 존재하지 않습니다. paymentId=" + paymentId
                ));

        return getCapturedAtOrThrow(payment);
    }

    /**
     * payment 엔티티 기준으로 capturedAt을 조회한다.
     */
    public LocalDateTime getCapturedAtOrThrow(Payment payment) {
        if (payment == null) {
            throw new IllegalArgumentException("payment는 필수입니다.");
        }

        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            throw new IllegalStateException(
                    "CAPTURED 상태가 아닌데 capturedAt 조회 시도. paymentId=" + payment.getPaymentId()
            );
        }

        return paymentEventRepository
                .findOccurredAtByPaymentIdAndEventType(
                        payment.getPaymentId(),
                        PaymentEventType.PAYMENT_CAPTURED
                )
                .orElseThrow(() -> new IllegalStateException(
                        "CAPTURE 이벤트가 존재하지 않습니다. paymentId=" + payment.getPaymentId()
                ));
    }
}