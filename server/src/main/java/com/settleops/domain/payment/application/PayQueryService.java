package com.settleops.domain.payment.application;

import com.settleops.domain.payment.domain.IdempotencyRecord;
import com.settleops.domain.payment.domain.Payment;
import com.settleops.domain.payment.domain.PaymentEventType;
import com.settleops.domain.payment.domain.PaymentStatus;
import com.settleops.domain.payment.infra.IdempotencyRecordRepository;
import com.settleops.domain.payment.infra.PaymentEventRepository;
import com.settleops.domain.payment.infra.PaymentRepository;
import com.settleops.global.enums.IdempotencyTargetType;
import com.settleops.global.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayQueryService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;

    /**
     * orderId에 연결된 payment를 조회한다.
     *
     * <p>현재 구조에서는 order 생성 시 payment가 CREATED 상태로 선생성되는 것이 전제다.</p>
     * <p>따라서 order는 존재하지만 payment가 없으면, 단순 미생성 케이스가 아니라
     * 주문 생성 유스케이스 정합이 깨진 비정상 상태로 본다.</p>
     * <p>외부 응답은 NotFound로 처리하되, 의미상으로는
     * "선생성되어 있어야 할 payment가 누락된 상태"를 나타낸다.</p>
     */
    @Transactional(readOnly = true)
    public Payment findPaymentByOrderIdOrThrow(String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("order에 연결된 payment가 존재하지 않습니다."));
    }

    /**
     * 멱등키 기준으로 "성공 완료된" 기존 결제를 조회한다.
     *
     * <p>현재 PAY 멱등은 성공 결과 저장형 기준이다.</p>
     * <p>즉, idempotency_record는 성공적으로 처리 완료된 요청 결과만
     * 유효한 멱등 성공 이력으로 간주한다.</p>
     * <p>해당 키의 성공 이력이 없으면 Optional.empty()를 반환한다.</p>
     * <p>성공 완료 record인데 참조 payment가 없으면 정합성 오류로 간주한다.</p>
     */
    public Optional<Payment> findSucceededIdempotentPayment(
            IdempotencyTargetType targetType,
            String targetId,
            String idempotencyKey
    ) {
        return idempotencyRecordRepository
                .findByTargetTypeAndTargetIdAndIdempotencyKey(targetType, targetId, idempotencyKey)
                .filter(this::isSucceededRecord)
                .map(record -> paymentRepository.findById(record.getPaymentId())
                        .orElseThrow(() -> new IllegalStateException(
                                "idempotency_record는 성공 완료 상태지만 참조 payment가 없습니다. " +
                                        "targetType=" + targetType +
                                        ", targetId=" + targetId +
                                        ", idempotencyKey=" + idempotencyKey
                        )));
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

    private boolean isSucceededRecord(IdempotencyRecord record) {
        return record.getPaymentId() != null
                && record.getResponseStatus() != null
                && record.getResponseStatus() == 200;
    }
}