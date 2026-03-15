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
import com.settleops.global.audit.AuditMetaFactory;
import com.settleops.global.audit.NoOpReason;
import com.settleops.global.db.DbConstraintUtils;
import com.settleops.global.enums.Action;
import com.settleops.global.enums.IdempotencyTargetType;
import com.settleops.global.enums.ReasonCode;
import com.settleops.global.error.BadRequestException;
import com.settleops.global.error.ConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final OrderService orderService;
    private final PayQueryService payQueryService;
    private final AuditLogger auditLogger;

    /**
     * 주문 결제를 수행한다.
     *
     * <p>동일 (orderId, X-Idempotency-Key) 재시도만 no-op 200으로 허용한다.</p>
     * <p>capturedAt의 SoT는 PAYMENT_CAPTURED 이벤트의 occurred_at이다.</p>
     * <p>pay 성공 시 order.status는 PAID로 전이된다.</p>
     */
    @Transactional
    public PayResponseDTO pay(String orderId, String idempotencyKey, String requestId) {

        // 1. 필수 파라미터 검증
        validateInputs(orderId, idempotencyKey);

        final IdempotencyTargetType targetType = IdempotencyTargetType.PAY_ORDER;

        // 2. 동일 (orderId, idempotencyKey) 성공 이력이 이미 있으면 현재 결과 반환(no-op 200)
        Payment idempotent = payQueryService
                .findSucceededIdempotentPayment(targetType, orderId, idempotencyKey)
                .orElse(null);

        if (idempotent != null) {
            LocalDateTime capturedAt = payQueryService.getCapturedAtOrThrow(idempotent);
            logPay(
                    idempotent,
                    requestId,
                    capturedAt,
                    PaymentStatus.CAPTURED,
                    PaymentStatus.CAPTURED,
                    true,
                    NoOpReason.IDEMPOTENT_REPLAY
            );
            return PayResponseDTO.from(idempotent, capturedAt);
        }

        // 3. 주문 조회
        Orders orders = orderService.getByOrderId(orderId);
        assertOrderNotPaid(orders);

        // 4. 신규 Payment 엔티티 생성
        Payment newPayment = Payment.create(
                orders.getOrderId(),
                orders.getMerchantId(),
                orders.getBuyerId(),
                orders.getAmount()
        );

        // 5. payment insert 시 UNIQUE 충돌이면 기존 payment로 수렴
        Payment payment = createPaymentOrConverge(newPayment, orderId);
        boolean convergedToExisting = !payment.getPaymentId().equals(newPayment.getPaymentId());

        if (convergedToExisting) {
            if (payment.getStatus() == PaymentStatus.CAPTURED) {
                LocalDateTime capturedAt = payQueryService.getCapturedAtOrThrow(payment);
                logPay(
                        payment,
                        requestId,
                        capturedAt,
                        PaymentStatus.CAPTURED,
                        PaymentStatus.CAPTURED,
                        true,
                        NoOpReason.IDEMPOTENT_REPLAY
                );
                return PayResponseDTO.from(payment, capturedAt);
            }
            throw new ConflictException(ReasonCode.IN_PROGRESS, "결제가 진행 중입니다.");
        }

        // 6. PAYMENT_EVENT :: CREATED (insert-only, duplicate ignore)
        saveEventIgnoreDuplicate(PaymentEvent.created(payment.getPaymentId(), requestId));

        PaymentStatus paymentStatusBefore = payment.getStatus();

        // 7. CAPTURE 수행 (도메인 로직)
        payment.capture();

        // 8. flush 보장 (UPDATE SQL 실행 시점 확정)
        payment = paymentRepository.saveAndFlush(payment);

        // 9. PAYMENT_EVENT :: CAPTURED (duplicate면 DB 상태로 수렴)
        CaptureResult captureResult = saveCapturedEventOrConverge(payment, requestId);
        payment = captureResult.payment();

        // 10. capturedAt 확정
        LocalDateTime capturedAt = payQueryService.getCapturedAtOrThrow(payment);

        // 11. audit_log 적재
        // - PAY에서는 PAYMENT_CAPTURED만 감사 로그로 남김
        // - PAYMENT_CREATED는 payment_event로 대체
        logPay(
                payment,
                requestId,
                capturedAt,
                captureResult.converged() ? PaymentStatus.CAPTURED : paymentStatusBefore,
                payment.getStatus(),
                captureResult.converged(),
                captureResult.converged() ? NoOpReason.CAPTURED_EVENT_DUPLICATE_CONVERGED : null
        );

        // 12. 주문 상태 PAID 전이
        orderService.markPaid(orderId);

        log.info("PAY 성공. orderId={}, paymentId={}, amount={}",
                orderId, payment.getPaymentId(), payment.getRequestedAmount());

        // 13. 성공 결과를 idempotency_record에 저장
        saveIdempotencySuccess(targetType, orderId, idempotencyKey, payment, requestId);

        // 14. 최종 응답 반환
        return PayResponseDTO.from(payment, capturedAt);
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
     * 성공 결과를 idempotency_record에 저장한다.
     *
     * <p>최초 성공 시 insert된다.</p>
     * <p>동일 키 중복 insert는 이미 저장된 성공 이력으로 간주하고 무시한다.</p>
     */
    private void saveIdempotencySuccess(
            IdempotencyTargetType targetType,
            String orderId,
            String idempotencyKey,
            Payment payment,
            String requestId
    ) {
        try {
            idempotencyRecordRepository.saveAndFlush(
                    IdempotencyRecord.success(
                            targetType,
                            orderId,
                            idempotencyKey,
                            payment.getPaymentId(),
                            200,
                            requestId
                    )
            );
        } catch (DataIntegrityViolationException e) {
            if (!DbConstraintUtils.isDuplicateKey(e)) {
                throw e;
            }

            log.info("IDEMPOTENCY_RECORD 중복 무시. orderId={}, idempotencyKey={}", orderId, idempotencyKey);
        }
    }

    /**
     * payment 생성 시 UNIQUE 충돌이 발생하면 기존 payment로 수렴한다.
     *
     * <p>동시성 상황에서 orderId UNIQUE 제약으로 인해 insert 충돌이 발생할 수 있다.</p>
     */
    private Payment createPaymentOrConverge(Payment payment,
                                            String orderId) {
        try {
            return paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException e) {
            if (!DbConstraintUtils.isDuplicateKey(e)) throw e;

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
            paymentEventRepository.saveAndFlush(event);
        } catch (DataIntegrityViolationException e) {
            if (DbConstraintUtils.isDuplicateKey(e)) {
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
    private CaptureResult saveCapturedEventOrConverge(Payment payment, String requestId) {
        try {
            paymentEventRepository.saveAndFlush(PaymentEvent.captured(payment.getPaymentId(), requestId));
            return new CaptureResult(payment, false);
        } catch (DataIntegrityViolationException e) {
            if (!DbConstraintUtils.isDuplicateKey(e)) throw e;

            Payment dbPayment = paymentRepository.findById(payment.getPaymentId())
                    .orElseThrow(() -> new IllegalStateException("payment 조회 실패"));

            if (dbPayment.getStatus() != PaymentStatus.CAPTURED) {
                throw new IllegalStateException("CAPTURED 이벤트 존재하지만 상태 불일치");
            }

            return new CaptureResult(dbPayment, true);
        }
    }

    /**
     * PAYMENT_CAPTURED 감사 로그를 기록한다.
     *
     * <p>최초 성공은 CREATED → CAPTURED로 기록한다.</p>
     * <p>동일 멱등키 재시도 및 동시 요청 수렴은 CAPTURED → CAPTURED + noOp=true 로 기록한다.</p>
     * <p>occurredAt은 PAYMENT_CAPTURED 이벤트의 occurred_at을 사용한다.</p>
     */
    private void logPay(
            Payment payment,
            String requestId,
            LocalDateTime capturedAt,
            PaymentStatus statusBefore,
            PaymentStatus statusAfter,
            boolean noOp,
            NoOpReason noOpReason
    ) {
        auditLogger.log(
                AuditLogCommand.builder()
                        .requestId(requestId)
                        .action(Action.PAYMENT_CAPTURED)
                        .actorType(ActorType.BUYER)
                        .actorId(payment.getBuyerId())
                        .entityType(EntityType.PAYMENT)
                        .entityId(payment.getPaymentId())
                        .statusBefore(statusBefore.name())
                        .statusAfter(statusAfter.name())
                        .merchantId(payment.getMerchantId())
                        .occurredAt(capturedAt)
                        .metaJson(buildPayMetaJson(noOp, noOpReason))
                        .build()
        );
    }

    private String buildPayMetaJson(boolean noOp, NoOpReason noOpReason) {
        if (!noOp) {
            return AuditMetaFactory.success().toString();
        }

        if (noOpReason == null) {
            throw new IllegalArgumentException("noOpReason must not be null when noOp is true");
        }

        return AuditMetaFactory.noOp(noOpReason, true).toString();
    }

    /**
     * 주문이 이미 PAID 상태인지 검증한다.
     *
     * <p>pay는 order.status가 CREATED인 경우에만 허용한다.</p>
     * <p>이미 PAID이면 새로운 결제 요청으로 간주하고 409를 반환한다.</p>
     */
    private static void assertOrderNotPaid(Orders orders) {
        if (orders.getStatus() == OrderStatus.PAID) {
            throw new ConflictException(ReasonCode.PAID_ALREADY, "이미 PAID 상태입니다.");
        }
    }

    private record CaptureResult(
            Payment payment,
            boolean converged
    ) { }
}