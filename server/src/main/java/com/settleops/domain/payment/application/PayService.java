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
import com.settleops.global.enums.Action;
import com.settleops.global.enums.IdempotencyTargetType;
import com.settleops.global.error.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("X-Idempotency-Key는 필수입니다.");
        }

        Optional<IdempotencyRecord> recordOpt = idempotencyRecordRepository.findByTargetTypeAndTargetIdAndIdempotencyKey(
                IdempotencyTargetType.PAY_ORDER,
                orderId,
                idempotencyKey
        );

// ------ 0) 이미 처리된 요청인 경우
        if(recordOpt.isPresent()) {
            IdempotencyRecord idempotencyRecord = recordOpt.get();
            Payment payment = paymentRepository.findById( // find by ~~~ 설정필요!
                        idempotencyRecord.getPaymentId()
                    ).orElseThrow(()->(new IllegalStateException("결제정보를 찾을 수 없습니다."))); // 데이터 정합성방어용 코드
            // 기존 응답 재반환 no-op 200 *** 캡쳐시간 타 테이블에서 조회 필요
            return PayResponseDTO.from(payment);
        }

// ------ 1) 신규 결제 처리
        Orders orders = orderService.getByOrderId(orderId);
        if (orders.getStatus() == OrderStatus.PAID) { // 기존데이터 충돌 방지용
            // 1-1. 기존 payment 조회 (1:1 전제)
            Payment payment = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() ->
                            new IllegalStateException("PAID 상태인데 payment가 없습니다. 데이터 정합성 오류")
                    );
            // 1-2. idempotency_record 복구 저장 (동시성 대비 try-catch)
            try {
                IdempotencyRecord record = IdempotencyRecord.create(
                        IdempotencyTargetType.PAY_ORDER,
                        orderId,
                        idempotencyKey,
                        payment.getPaymentId(),
                        200
                );
                idempotencyRecordRepository.save(record);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // UNIQUE 충돌 → 이미 다른 요청이 먼저 저장한 것
                // 아무것도 하지 않음
            }
            log.warn("Idempotency 복구 발생 - orderId={}", orderId);
            // 1-3. 기존 결과 반환 (no-op 200)
            return PayResponseDTO.from(payment);
        }

        Payment payment = Payment.create(
                orders.getOrderId(),
                orders.getMerchantId(),
                orders.getBuyerId(),
                orders.getAmount()
        );

        paymentRepository.save(payment);

        // 1) PAYMENT_CREATED event (before=null, after=CREATED)
        paymentEventRepository.save(
                PaymentEvent.created(payment.getPaymentId()) // 내부에서 requestId(MDC) 주입
        );
        payment.capture(); // status=CAPTURED

        // 2) PAYMENT_CAPTURED event (before=CREATED, after=CAPTURED)
        paymentEventRepository.save(
                PaymentEvent.captured(payment.getPaymentId())
        );

        // payment 상태 저장 반영
        paymentRepository.save(payment);

        // order 상태 전이 (OrderService에게 위임)
        orderService.markPaid(orders);


// ------ 2. idempotency_record 저장
        IdempotencyRecord record = IdempotencyRecord.create(
                IdempotencyTargetType.PAY_ORDER,
                orderId,
                idempotencyKey,
                payment.getPaymentId(),
                200
        );
        idempotencyRecordRepository.save(record);

        return PayResponseDTO.from(payment);
    }


}
