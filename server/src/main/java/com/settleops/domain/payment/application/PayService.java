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
import com.settleops.global.db.DbConstraintUtils;
import com.settleops.global.enums.IdempotencyTargetType;
import com.settleops.global.error.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final OrderService orderService;

    /**
    * <p>1. `X-Idempotency-Key` 헤더가 들어왔는지 확인 (없으면 400).</p>
    * <p>2. DB에서 `(orderId, idempotencyKey)` 조합으로 이미 성공한 기록이 있는지 조회.</p>
    * <p>3. 있다면 실제 결제 로직을 타지 않고 **기존 결과 반환 (no-op 200)**.</p>
    * <p>4. 없다면 결제 처리 후 `payment_event`에 기록. </p>
    */
    @Transactional
    public PayResponseDTO pay(String orderId, String idempotencyKey) {

        // 입력 검증(400)
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("X-Idempotency-Key는 필수입니다.");
        }
        if (orderId == null || orderId.isBlank()) {
            throw new BadRequestException("orderId는 필수입니다.");
        }

        final IdempotencyTargetType idempotencytype = IdempotencyTargetType.PAY_ORDER;

// ------ 0) 이미 처리된 요청인 경우
        Optional<IdempotencyRecord> recordOpt = idempotencyRecordRepository.findByTargetTypeAndTargetIdAndIdempotencyKey(
                idempotencytype,
                orderId,
                idempotencyKey
        );

        if(recordOpt.isPresent()) {
            IdempotencyRecord idempotencyRecord = recordOpt.get();
            Payment payment = paymentRepository.findById( // find by ~~~ 설정필요!
                        idempotencyRecord.getPaymentId()
                    ).orElseThrow(()->(new IllegalStateException("멱등 레코드가 가리키는 payment가 없습니다. 정합성 오류"))); // 데이터 정합성방어용 코드
            // 기존 응답 재반환 no-op 200 *** 캡쳐시간 타 테이블에서 조회 필요
            return PayResponseDTO.from(payment);
        }

// ------ 1) 신규 결제 처리
        Orders orders = orderService.getByOrderId(orderId);
        if (orders.getStatus() == OrderStatus.PAID) { // 기존데이터 충돌 방지용
            // 1-1. 기존 payment 조회 (1:1 전제)
            Payment existingPayment = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new IllegalStateException("order가 PAID인데 payment가 없습니다. 정합성 오류"));

            // 1-2. idempotency_record 복구 저장 (동시성 대비 try-catch)
            try {
                idempotencyRecordRepository.save(
                        IdempotencyRecord.create(
                                idempotencytype,
                                orderId,
                                idempotencyKey,
                                existingPayment.getPaymentId(),
                                200)
                );
            } catch (DataIntegrityViolationException e) {
                if (DbConstraintUtils.isDuplicateKey(e)) {
                    log.info("IDEMPOTENCY_RECORD 복구 저장 중복(UNIQUE)으로 무시. orderId={}, paymentId={}, cause={}",
                            orderId, existingPayment.getPaymentId(), DbConstraintUtils.rootMessage(e));
                } else {
                    log.error("IDEMPOTENCY_RECORD 복구 저장 실패(UNIQUE 외). orderId={}, paymentId={}, cause={}",
                            orderId, existingPayment.getPaymentId(), DbConstraintUtils.rootMessage(e), e);
                    throw e;
                }
            }
            return PayResponseDTO.from(existingPayment);
        }

// ------ 2) PAYMENT_CREATED
        Payment payment = Payment.create(
                orders.getOrderId(),
                orders.getMerchantId(),
                orders.getBuyerId(),
                orders.getAmount()
        );

        try {
            paymentRepository.save(payment);
        } catch (DataIntegrityViolationException e) {
            if (!DbConstraintUtils.isDuplicateKey(e)) {
                log.error("PAYMENT 저장 실패(UNIQUE 외). orderId={}, cause={}",
                        orderId, DbConstraintUtils.rootMessage(e), e);
                throw e;
            }

            log.info("PAYMENT 생성 중복(UNIQUE)으로 기존 payment로 수렴. orderId={}, cause={}",
                    orderId, DbConstraintUtils.rootMessage(e));
            Payment existingPayment = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> e);  // duplicate인데 조회가 안되면 비정상

            // 멱등레코드 best-effort 저장: duplicate만 무시
            try {
                idempotencyRecordRepository.save(
                        IdempotencyRecord.create(
                                idempotencytype,
                                orderId,
                                idempotencyKey,
                                existingPayment.getPaymentId(),
                                200
                        )
                );
            } catch (DataIntegrityViolationException ex) {
                if (DbConstraintUtils.isDuplicateKey(ex)) {
                    log.info("IDEMPOTENCY_RECORD best-effort 저장 중복(UNIQUE)으로 무시. orderId={}, paymentId={}, cause={}",
                            orderId, existingPayment.getPaymentId(), DbConstraintUtils.rootMessage(ex));
                } else {
                    log.error("IDEMPOTENCY_RECORD best-effort 저장 실패(UNIQUE 외). orderId={}, paymentId={}, cause={}",
                            orderId, existingPayment.getPaymentId(), DbConstraintUtils.rootMessage(ex), ex);
                    throw ex;
                }
            }
            return PayResponseDTO.from(existingPayment);
        }

// ------ 3) PAYMENT_EVENT[insert-only] :: CREATED (before=null, after=CREATED)
        try {
            // 내부에서 requestId(MDC) 주입
            paymentEventRepository.save(PaymentEvent.created(payment.getPaymentId()));
        }catch (DataIntegrityViolationException e) {
            if (DbConstraintUtils.isDuplicateKey(e)) {
                log.info("PAYMENT_EVENT CREATED 중복(UNIQUE)으로 무시. paymentId={}, cause={}",
                        payment.getPaymentId(), DbConstraintUtils.rootMessage(e));
            } else {
                log.error("PAYMENT_EVENT CREATED 저장 실패(UNIQUE 외). paymentId={}, cause={}",
                        payment.getPaymentId(), DbConstraintUtils.rootMessage(e), e);
                throw e;
            }
        }

// ------ 4) CAPTURE(도메인에서 amount/status 확정)
        payment.capture(); // status=CAPTURED, capturedAmount=requestedAmount
        log.info("PAY 성공. orderId={}, paymentId={}, amount={}", orderId, payment.getPaymentId(), payment.getRequestedAmount());

// ------ 5) PAYMENT_EVENT[insert-only] :: CAPTURED (before=CREATED, after=CAPTURED)
        try {
            paymentEventRepository.save(PaymentEvent.captured(payment.getPaymentId()));
            // transaction commit 시 payment update 자동반영
        } catch (DataIntegrityViolationException e) {

            if (!DbConstraintUtils.isDuplicateKey(e)) {
                log.error("PAYMENT_EVENT CAPTURED 저장 실패(UNIQUE 외). paymentId={}, cause={}",
                        payment.getPaymentId(), DbConstraintUtils.rootMessage(e), e);
                throw e;
            }// duplicate인 경우만 수렴 로직

            // 이미 CAPTURED 이벤트가 존재(=다른 요청이 먼저 적재) → payment 상태 확정 검증(재조회 1회)
            Payment dbPayment = paymentRepository.findById(payment.getPaymentId())
                    .orElseThrow(() -> new IllegalStateException("payment가 없습니다. 정합성 오류"));

            if (dbPayment.getStatus() != PaymentStatus.CAPTURED) {
                throw new IllegalStateException("CAPTURED 이벤트는 있는데 payment.status가 CAPTURED가 아닙니다. 정합성 오류");
            }
            if (dbPayment.getCapturedAmount() != dbPayment.getRequestedAmount()) {
                throw new IllegalStateException("CAPTURED인데 capturedAmount != requestedAmount 입니다. 정합성 오류");
            }

            // 이 요청의 payment 객체는 stale일 수 있으니, 이후 응답/흐름은 dbPayment로 수렴하는게 안전
            payment = dbPayment;

            log.info("PAYMENT_CAPTURED 이벤트 중복(UNIQUE). 이미 처리됨으로 수렴. paymentId={}", payment.getPaymentId());
        }


// ------ 7) order 상태 전이 (OrderService에게 위임)
        orderService.markPaid(orderId);

// ------ 8). idempotency_record 저장 (UNIQUE 충돌 시 기존 레코드로 수렴)
        try{
            idempotencyRecordRepository.save(
                IdempotencyRecord.create(
                        idempotencytype, // Locked
                        orderId,
                        idempotencyKey,
                        payment.getPaymentId(),
                        200
                )
            );
        } catch (DataIntegrityViolationException e) {
            // 이미 응답 처리 된 payment 라면 200 결과 재반환
            if (DbConstraintUtils.isDuplicateKey(e)) {
                log.info("IDEMPOTENCY_RECORD 중복(UNIQUE)으로 수렴. orderId={}, paymentId={}, cause={}",
                        orderId, payment.getPaymentId(), DbConstraintUtils.rootMessage(e));

                IdempotencyRecord existing =
                    idempotencyRecordRepository.findByTargetTypeAndTargetIdAndIdempotencyKey(idempotencytype, orderId, idempotencyKey)
                        .orElseThrow(() -> e);

                Payment existingPayment = paymentRepository.findById(existing.getPaymentId())
                        .orElseThrow(() -> new IllegalStateException("멱등 레코드가 가리키는 payment가 없습니다. 정합성 오류"));

                return PayResponseDTO.from(existingPayment);
            }
            log.error("IDEMPOTENCY_RECORD 저장 실패(UNIQUE 외). orderId={}, paymentId={}, cause={}",
                    orderId, payment.getPaymentId(), DbConstraintUtils.rootMessage(e), e);
            throw e;
        }

        return PayResponseDTO.from(payment);
    }


}
