package com.settleops.domain.payment.application;

import com.settleops.domain.order.application.OrderService;
import com.settleops.domain.order.domain.OrderStatus;
import com.settleops.domain.order.domain.Orders;
import com.settleops.domain.payment.api.dto.PayResponseDTO;
import com.settleops.domain.payment.domain.IdempotencyRecord;
import com.settleops.domain.payment.domain.Payment;
import com.settleops.domain.payment.domain.PaymentEvent;
import com.settleops.domain.payment.domain.PaymentStatus;
import com.settleops.domain.payment.infra.IdempotencyRecordRepository;
import com.settleops.domain.payment.infra.PaymentEventRepository;
import com.settleops.domain.payment.infra.PaymentRepository;
import com.settleops.global.audit.ActorType;
import com.settleops.global.audit.AuditLogCommand;
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.audit.EntityType;
import com.settleops.global.db.DbConstraintUtils;
import com.settleops.global.enums.Action;
import com.settleops.global.enums.IdempotencyTargetType;
import com.settleops.global.enums.ReasonCode;
import com.settleops.global.error.BadRequestException;
import com.settleops.global.error.ConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.settleops.global.logging.RequestIdKeys.MDC_KEY;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final OrderService orderService;

    private final AuditLogger auditLogger;

    /**
     * 결제 진입점.
     *
     * <p><b>처리 순서</b></p>
     * <ol>
     *     <li>입력값 검증</li>
     *     <li>멱등키 기반 기존 성공 요청 조회 (no-op 처리)</li>
     *     <li>order=PAID 이지만 결제 시도 시 409 처리</li>
     *     <li>Payment 생성 (UNIQUE 충돌 시 기존 payment로 수렴)</li>
     *     <li>CREATED 이벤트 적재 (insert-only)</li>
     *     <li>capture 수행 + flush 보장</li>
     *     <li>CAPTURED 이벤트 적재 (중복 시 DB 상태로 수렴)</li>
     *     <li>Order 상태 전이</li>
     *     <li>멱등 레코드 저장 (중복 시 기존 payment로 수렴)</li>
     * </ol>
     */
    @Transactional
    public PayResponseDTO pay(String orderId, String idempotencyKey) {

        // 1. 필수 파라미터 검증
        validateInputs(orderId, idempotencyKey);

        final IdempotencyTargetType targetType = IdempotencyTargetType.PAY_ORDER;

        // 2. 멱등키 기반 기존 성공 요청 조회 (이미 처리된 경우 즉시 반환)
        Payment idempotent = findIdempotentPaymentOrNull(targetType, orderId, idempotencyKey);
        if (idempotent != null) {
            return PayResponseDTO.from(idempotent);
        }

        // 3. 주문 조회
        Orders orders = orderService.getByOrderId(orderId);
        assertOrderNotPaid(orders);

        // 5. 신규 Payment 엔티티 생성
        Payment newPayment = Payment.create(
                orders.getOrderId(),
                orders.getMerchantId(),
                orders.getBuyerId(),
                orders.getAmount()
        );

        // 6. payment insert 시 UNIQUE 충돌이면 기존 payment로 수렴
        Payment payment = createPaymentOrConverge(newPayment, orderId);
        if (payment != newPayment) {
            if (payment.getStatus() != PaymentStatus.CAPTURED) {
                throw new ConflictException(ReasonCode.IN_PROGRESS, "결제가 진행 중입니다.");
            }
            // CAPTURED가 확정된 경우에만 멱등 저장
            saveIdempotencyIgnoreDuplicate(targetType, orderId, idempotencyKey, payment.getPaymentId());
            return PayResponseDTO.from(payment);
        }

        // 7. PAYMENT_EVENT :: CREATED (insert-only, duplicate ignore)
        saveEventIgnoreDuplicate(PaymentEvent.created(payment.getPaymentId()));

        PaymentStatus paymentStatusBefore = payment.getStatus();

        // 8. CAPTURE 수행 (도메인 로직)
        payment.capture();

        // 9. flush 보장 (UPDATE SQL 실행 시점 확정)
        payment = paymentRepository.saveAndFlush(payment);

        // 10. PAYMENT_EVENT :: CAPTURED (duplicate면 DB 상태로 수렴)
        payment = saveCapturedEventOrConverge(payment);

        // MVP: pay에서는 PAYMENT_CAPTURED만 audit (PAYMENT_CREATED는 payment_event로 대체)
        auditLogger.log(AuditLogCommand.builder()
                .requestId(MDC.get(MDC_KEY))            // MDC에서 전역 requestId 획득
                .action(Action.PAYMENT_CAPTURED)            // 기획서 명시 액션 코드
                .actorType(ActorType.BUYER)                 // 또는 SYSTEM
                .actorId(orders.getBuyerId())
                .entityType(EntityType.PAYMENT)
                .entityId(payment.getPaymentId())
                .statusBefore(paymentStatusBefore.name())   // 상태 전이 기록
                .statusAfter(payment.getStatus().name())
                .merchantId(orders.getMerchantId())
                .occurredAt(LocalDateTime.now())
                .metaJson("{}")                             // 필요 시 추가 상세 데이터
                .build());

        log.info("PAY 성공. orderId={}, paymentId={}, amount={}",
                orderId, payment.getPaymentId(), payment.getRequestedAmount());

        // 11. 주문 상태 PAID 전이
        orderService.markPaid(orderId);

        // 12. idempotency_record 저장 (duplicate면 기존 payment로 수렴)
        Payment finalPayment = saveIdempotencyOrConverge(targetType, orderId, idempotencyKey, payment);

        return PayResponseDTO.from(finalPayment);
    }

    /**
     * 입력값 검증.
     *
     * <p>orderId와 idempotencyKey는 필수값이다.</p>
     * <p>누락 시 400 BadRequestException 발생.</p>
     */
    private static void validateInputs(String orderId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("X-Idempotency-Key는 필수입니다.");
        }
        if (orderId == null || orderId.isBlank()) {
            throw new BadRequestException("orderId는 필수입니다.");
        }
    }

    /**
     * 중복키 예외 판별.
     *
     * <p>DB UNIQUE 제약 위반 여부를 판별한다.</p>
     */
    private boolean isDuplicate(DataIntegrityViolationException e) {
        return DbConstraintUtils.isDuplicateKey(e);
    }

    /**
     * payment 생성 시 UNIQUE 충돌이 발생하면 기존 payment로 수렴한다.
     *
     * <p>동시성 상황에서 orderId UNIQUE 제약으로 인해 insert 충돌이 발생할 수 있다.</p>
     */
    private Payment createPaymentOrConverge(Payment payment,
                                            String orderId) {
        try {
            return paymentRepository.save(payment);
        } catch (DataIntegrityViolationException e) {
            if (!isDuplicate(e)) throw e;

            log.info("PAYMENT 생성 중복. orderId={}", orderId);

            Payment existing = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new IllegalStateException("기존 payment 조회 실패"));
            return existing; // UNIQUE 충돌
        }
    }

    /**
     * payment 이벤트 insert-only 저장.
     *
     * <p>UNIQUE 중복은 무시하고, 그 외 예외는 전파한다.</p>
     */
    private void saveEventIgnoreDuplicate(PaymentEvent event) {
        try {
            paymentEventRepository.save(event);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicate(e)) {
                log.info("EVENT 중복 무시. paymentId={}", event.getPaymentId());
                return;
            }
            throw e;
        }
    }

    /**
     * CAPTURED 이벤트 적재.
     *
     * <p>이미 존재하는 경우 DB의 payment 상태를 재조회하여 수렴한다.</p>
     */
    private Payment saveCapturedEventOrConverge(Payment payment) {
        try {
            paymentEventRepository.save(PaymentEvent.captured(payment.getPaymentId()));
            return payment;
        } catch (DataIntegrityViolationException e) {
            if (!isDuplicate(e)) throw e;

            Payment dbPayment = paymentRepository.findById(payment.getPaymentId())
                    .orElseThrow(() -> new IllegalStateException("payment 조회 실패"));

            if (dbPayment.getStatus() != PaymentStatus.CAPTURED) {
                throw new IllegalStateException("CAPTURED 이벤트 존재하지만 상태 불일치");
            }

            return dbPayment;
        }
    }

    /**
     * 멱등 레코드 저장.
     *
     * <p>이미 존재하는 경우 기존 레코드가 가리키는 payment로 수렴한다.</p>
     */
    private Payment saveIdempotencyOrConverge(IdempotencyTargetType targetType,
                                              String orderId,
                                              String idempotencyKey,
                                              Payment payment) {
        try {
            idempotencyRecordRepository.save(
                    IdempotencyRecord.create(
                            targetType,
                            orderId,
                            idempotencyKey,
                            payment.getPaymentId(),
                            200
                    )
            );
            return payment;
        } catch (DataIntegrityViolationException e) {
            if (!isDuplicate(e)) throw e;

            IdempotencyRecord existing = idempotencyRecordRepository
                    .findByTargetTypeAndTargetIdAndIdempotencyKey(targetType, orderId, idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException("멱등 레코드 조회 실패"));

            return paymentRepository.findById(existing.getPaymentId())
                    .orElseThrow(() -> new IllegalStateException("payment 조회 실패"));
        }
    }

    /**
     * 멱등 레코드 best-effort 저장.
     *
     * <p>중복은 무시하고 그 외 예외는 전파한다.</p>
     */
    private void saveIdempotencyIgnoreDuplicate(IdempotencyTargetType targetType,
                                                String orderId,
                                                String idempotencyKey,
                                                String paymentId) {
        try {
            idempotencyRecordRepository.save(
                    IdempotencyRecord.create(
                            targetType,
                            orderId,
                            idempotencyKey,
                            paymentId,
                            200
                    )
            );
        } catch (DataIntegrityViolationException e) {
            if (!isDuplicate(e)) throw e;
        }
    }

    /**
     * 멱등키 기반 기존 성공 payment 조회.
     *
     * <p>존재하지 않으면 null 반환.</p>
     */
    private Payment findIdempotentPaymentOrNull(IdempotencyTargetType targetType,
                                                String orderId,
                                                String idempotencyKey) {

        Optional<IdempotencyRecord> record =
                idempotencyRecordRepository
                        .findByTargetTypeAndTargetIdAndIdempotencyKey(targetType, orderId, idempotencyKey);

        if (record.isEmpty()) return null;

        return paymentRepository.findById(record.get().getPaymentId())
                .orElseThrow(() -> new IllegalStateException(
                        "idempotency_record는 존재하지만 참조 payment가 없습니다. 정합성 오류"
                ));
    }

    /**
     * 주문이 이미 PAID이면 새 결제 요청을 차단한다.
     *
     * <p>동일 (orderId, idempotencyKey) 재시도는 2단계에서 이미 no-op 200으로 반환되므로,
     * 여기서는 '멱등 레코드 없음 + order=PAID' = 새 멱등키 재요청(상태 위반)만 처리한다.</p>
     */
    private static void assertOrderNotPaid(Orders orders) {
        if (orders.getStatus() == OrderStatus.PAID) {
            throw new ConflictException(ReasonCode.PAID_ALREADY, "이미 PAID 상태입니다.");
        }
    }
}
