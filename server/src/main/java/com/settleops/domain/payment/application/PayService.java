package com.settleops.domain.payment.application;

import com.settleops.domain.order.application.OrderService;
import com.settleops.domain.order.domain.OrderStatus;
import com.settleops.domain.order.domain.Orders;
import com.settleops.domain.payment.api.dto.PayResponseDTO;
import com.settleops.domain.payment.domain.Payment;
import com.settleops.domain.payment.domain.PaymentStatus;
import com.settleops.global.audit.ActorType;
import com.settleops.global.audit.AuditLogCommand;
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.audit.AuditMetaFactory;
import com.settleops.global.audit.EntityType;
import com.settleops.global.audit.NoOpReason;
import com.settleops.global.enums.Action;
import com.settleops.global.enums.IdempotencyTargetType;
import com.settleops.global.enums.ReasonCode;
import com.settleops.global.error.BadRequestException;
import com.settleops.global.error.ConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayService {

    private final OrderService orderService;
    private final PayQueryService payQueryService;
    private final PayPaymentWriter payPaymentWriter;
    private final PayEventWriter payEventWriter;
    private final PayIdempotencyWriter payIdempotencyWriter;
    private final AuditLogger auditLogger;

    /**
     * 주문 결제를 수행한다.
     *
     * <p>동일 (orderId, X-Idempotency-Key) 재시도만 no-op 200으로 허용한다.</p>
     * <p>capturedAt의 SoT는 PAYMENT_CAPTURED 이벤트의 occurred_at이다.</p>
     * <p>pay 성공 시 order.status는 CREATED → PAID 로 전이된다.</p>
     *
     * <p>현재 구조에서 payment는 order 생성 시점에 CREATED 상태로 미리 생성된다.</p>
     * <p>따라서 pay는 신규 payment를 생성하지 않고, 기존 payment를 조회해
     * CREATED → CAPTURED 상태 전이만 수행한다.</p>
     *
     * <p>멱등 처리 정책:
     * <br>- 본 구현은 요청 시작 시 멱등 row를 선점하는 claim 방식이 아니다.
     * <br>- 먼저 성공 이력(idempotency_record)을 조회하고, 이미 성공 결과가 있으면 no-op 200으로 반환한다.
     * <br>- 최초 성공 요청은 기존 payment의 상태 전이(CREATED → CAPTURED)와
     *     후행 성공 결과 저장(idempotency_record)으로 수렴한다.
     * <br>- 동일 요청이 동시 처리 중인 구간에서 write 충돌이 발생하면 기존 성공 결과로 즉시 수습하지 않고
     *     409 IN_PROGRESS 정책으로 수렴한다.
     * </p>
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
                    NoOpReason.ALREADY_CAPTURED
            );
            return PayResponseDTO.from(idempotent, capturedAt);
        }

        // 3. 주문 조회 및 상태 검증
        Orders orders = orderService.getByOrderId(orderId);
        assertOrderNotPaid(orders);

        // 4. order 생성 시 미리 만들어둔 payment 조회
        Payment payment = payQueryService.findPaymentByOrderIdOrThrow(orderId);

        // 5. pay는 CREATED 상태 payment에 대해서만 허용
        assertPaymentCreated(payment);

        PaymentStatus paymentStatusBefore = payment.getStatus();

        // 6. CAPTURE 수행
        payment.capture();

        // 7. payment 상태 저장
        // payment.capture() 이후 상태 반영 시점을 명시적으로 확정한다.
        // 현재 구조에서는 Dirty Checking만에 의존하지 않고 saveAndFlush로 DB 반영 시점을 고정한다.
        // 이후 CAPTURED 이벤트 적재 전에 payment.status=CAPTURED 정합을 분명히 맞추기 위함이다.
        payment = payPaymentWriter.saveCapturedState(payment);

        // 8. PAYMENT_EVENT :: CAPTURED
        payEventWriter.saveCaptured(payment.getPaymentId(), requestId);

        // 9. capturedAt 확정
        LocalDateTime capturedAt = payQueryService.getCapturedAtOrThrow(payment);

        // 10. audit_log 적재
        logPay(
                payment,
                requestId,
                capturedAt,
                paymentStatusBefore,
                payment.getStatus(),
                false,
                null
        );

        // 11. 주문 상태 PAID 전이
        orderService.markPaid(orderId);

        log.info("PAY 성공. orderId={}, paymentId={}, amount={}",
                orderId, payment.getPaymentId(), payment.getRequestedAmount());

        // 12. 성공 결과를 idempotency_record에 저장
        // 동일 요청에 대한 동시 처리 경합 상황에서는 이 단계에서 write 충돌이 발생할 수 있으며,
        // 해당 경우 409 IN_PROGRESS 정책으로 수렴한다.
        payIdempotencyWriter.saveSuccess(
                targetType,
                orderId,
                idempotencyKey,
                payment.getPaymentId(),
                requestId
        );

        // 13. 최종 응답 반환
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
     * PAYMENT_CAPTURED 감사 로그를 기록한다.
     *
     * <p>최초 성공은 CREATED → CAPTURED 로 기록한다.</p>
     * <p>동일 멱등키 재시도는 CAPTURED → CAPTURED + noOp=true 로 기록한다.</p>
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
     * <p>이미 PAID이면 409 ORDER_ALREADY_PAID 를 반환한다.</p>
     */
    private static void assertOrderNotPaid(Orders orders) {
        if (orders.getStatus() == OrderStatus.PAID) {
            throw new ConflictException(ReasonCode.ORDER_ALREADY_PAID, "이미 PAID 상태입니다.");
        }
    }

    /**
     * pay 대상 payment가 CREATED 상태인지 검증한다.
     *
     * <p>현재 구조에서 pay는 order 생성 시 선생성된 payment를 대상으로 수행한다.</p>
     * <p>이미 CAPTURED 상태이면 중복 승인 시도로 간주하고 409를 반환한다.</p>
     */
    private static void assertPaymentCreated(Payment payment) {
        if (payment.getStatus() != PaymentStatus.CREATED) {
            throw new ConflictException(ReasonCode.PAYMENT_ALREADY_CAPTURED, "이미 CAPTURED 상태입니다.");
        }
    }
}