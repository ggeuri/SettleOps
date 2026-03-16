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
     * <p>pay 성공 시 order.status는 PAID로 전이된다.</p>
     *
     * <p>멱등 처리 정책:
     * <br>- 본 구현은 요청 시작 시 멱등 row를 선점하는 claim 방식이 아니다.
     * <br>- 먼저 성공 이력(idempotency_record)을 조회하고, 이미 성공 결과가 있으면 no-op 200으로 반환한다.
     * <br>- 최초 성공 요청은 payment.order_id UNIQUE 제약과 후행 성공 결과 저장(idempotency_record)으로 수렴한다.
     * <br>- 따라서 현재 구조는 선점형 멱등이 아니라 성공 결과 저장형 + payment.order_id UNIQUE 수렴 방식이다.
     * <br>- 동일 요청이 동시 처리 중인 구간에서 write 충돌이 발생하면 기존 성공 결과로 즉시 수습하지 않고
     *     409 IN_PROGRESS + "잠시 후 다시 시도해주세요." 정책으로 수렴한다.
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

        // 5. payment 생성
        // - payment.order_id UNIQUE 충돌 등 write 시점 경합이 발생하면
        //   기존 성공 결과를 즉시 재사용하지 않고 동시 처리 중으로 간주한다.
        // - 이 경우 409 IN_PROGRESS + "잠시 후 다시 시도해주세요." 정책으로 수렴한다.
        Payment payment = payPaymentWriter.create(newPayment, orderId);

        // 6. PAYMENT_EVENT :: CREATED
        // - duplicate 발생 시 409 IN_PROGRESS
        payEventWriter.saveCreated(payment.getPaymentId(), requestId);

        PaymentStatus paymentStatusBefore = payment.getStatus();

        // 7. CAPTURE 수행
        payment.capture();

        // 8. payment 상태 저장
        // payment.capture() 이후 상태 반영 시점을 명시적으로 확정한다.
        // 현재 구조에서는 Dirty Checking만에 의존하지 않고 saveAndFlush로 DB 반영 시점을 고정한다.
        // 이후 CAPTURED 이벤트 적재 전에 payment.status=CAPTURED 정합을 분명히 맞추기 위함.

        payment = payPaymentWriter.saveCapturedState(payment);

        // 9. PAYMENT_EVENT :: CAPTURED
        // - duplicate 발생 시 409 IN_PROGRESS
        payEventWriter.saveCaptured(payment.getPaymentId(), requestId);

        // 10. capturedAt 확정
        LocalDateTime capturedAt = payQueryService.getCapturedAtOrThrow(payment);

        // 11. audit_log 적재
        logPay(
                payment,
                requestId,
                capturedAt,
                paymentStatusBefore,
                payment.getStatus(),
                false,
                null
        );

        // 12. 주문 상태 PAID 전이
        orderService.markPaid(orderId);

        log.info("PAY 성공. orderId={}, paymentId={}, amount={}",
                orderId, payment.getPaymentId(), payment.getRequestedAmount());

        // 13. 성공 결과를 idempotency_record에 저장
        // - 이 단계에서 duplicate가 발생하더라도 기존 성공 응답으로 재수습하지 않는다.
        // - 동일 요청에 대한 동시 처리 경합 상황으로 간주하고 409 IN_PROGRESS로 수렴한다.
        payIdempotencyWriter.saveSuccess(
                targetType,
                orderId,
                idempotencyKey,
                payment.getPaymentId(),
                requestId
        );

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
     * PAYMENT_CAPTURED 감사 로그를 기록한다.
     *
     * <p>최초 성공은 CREATED → CAPTURED로 기록한다.</p>
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
     * <p>이미 PAID이면 새로운 결제 요청으로 간주하고 409를 반환한다.</p>
     */
    private static void assertOrderNotPaid(Orders orders) {
        if (orders.getStatus() == OrderStatus.PAID) {
            throw new ConflictException(ReasonCode.PAID_ALREADY, "이미 PAID 상태입니다.");
        }
    }
}