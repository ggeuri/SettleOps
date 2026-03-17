package com.settleops.domain.payment.application;

import com.settleops.domain.order.domain.Orders;
import com.settleops.domain.order.infra.OrdersRepository;
import com.settleops.domain.payment.api.dto.PayResponseDTO;
import com.settleops.domain.payment.domain.IdempotencyRecord;
import com.settleops.domain.payment.domain.Payment;
import com.settleops.domain.payment.domain.PaymentStatus;
import com.settleops.domain.payment.infra.IdempotencyRecordRepository;
import com.settleops.domain.payment.infra.PaymentRepository;
import com.settleops.global.enums.IdempotencyTargetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class PayServiceIdempotencyTest {

    private static final String MERCHANT_ID = "MERCHANT_1";
    private static final String BUYER_ID = "BUYER_1";
    private static final String ITEM_NAME = "테스트 상품";
    private static final long AMOUNT = 10_000L;

    @Autowired
    private PayService payService;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Test
    @DisplayName("동일 idempotency key 재시도 시 기존 결과를 반환하고 payment/idempotency row는 재사용된다")
    void pay_same_idempotency_key_should_return_same_result_and_keep_rows_immutable() {
        // given
        String idempotencyKey = "idem-" + UUID.randomUUID();
        String requestId1 = UUID.randomUUID().toString();
        String requestId2 = UUID.randomUUID().toString();

        Orders order = Orders.create(
                MERCHANT_ID,
                BUYER_ID,
                ITEM_NAME,
                AMOUNT
        );
        order = ordersRepository.saveAndFlush(order);

        String orderId = order.getOrderId();

        // when: 첫 번째 결제
        PayResponseDTO first = payService.pay(orderId, idempotencyKey, requestId1);

        // then: 첫 번째 결제 결과 확인
        assertThat(first).isNotNull();
        assertThat(first.paymentId()).isNotBlank();
        assertThat(first.status()).isEqualTo("CAPTURED");
        assertThat(first.capturedAt()).isNotNull();

        // 현재 orderId 기준 payment row 확인
        Payment firstSavedPayment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new AssertionError("payment가 생성되지 않았습니다."));

        assertThat(firstSavedPayment.getPaymentId()).isEqualTo(first.paymentId());
        assertThat(firstSavedPayment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);

        // 현재 (targetType, orderId, idempotencyKey) 기준 idempotency row 확인
        Optional<IdempotencyRecord> firstRecordOpt =
                idempotencyRecordRepository.findByTargetTypeAndTargetIdAndIdempotencyKey(
                        IdempotencyTargetType.PAY_ORDER,
                        orderId,
                        idempotencyKey
                );

        assertThat(firstRecordOpt).isPresent();

        IdempotencyRecord firstRecord = firstRecordOpt.get();
        Long firstIdempotencyId = firstRecord.getIdempotencyId();
        LocalDateTime firstCreatedAt = firstRecord.getCreatedAt();

        assertThat(firstRecord.getPaymentId()).isEqualTo(first.paymentId());
        assertThat(firstRecord.getResponseStatus()).isEqualTo(200);
        assertThat(firstRecord.getRequestId()).isEqualTo(requestId1);

        // when: 동일 멱등키 재시도
        PayResponseDTO second = payService.pay(orderId, idempotencyKey, requestId2);

        // then: 동일 결과 반환
        assertThat(second).isNotNull();
        assertThat(second.paymentId()).isEqualTo(first.paymentId());
        assertThat(second.status()).isEqualTo(first.status());
        assertThat(second.capturedAt()).isEqualTo(first.capturedAt());

        // 동일 orderId 기준 payment row 재사용 확인
        Payment secondSavedPayment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new AssertionError("payment가 조회되지 않습니다."));

        assertThat(secondSavedPayment.getPaymentId()).isEqualTo(first.paymentId());
        assertThat(secondSavedPayment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);

        // 동일 idempotency row 재사용 확인
        Optional<IdempotencyRecord> secondRecordOpt =
                idempotencyRecordRepository.findByTargetTypeAndTargetIdAndIdempotencyKey(
                        IdempotencyTargetType.PAY_ORDER,
                        orderId,
                        idempotencyKey
                );

        assertThat(secondRecordOpt).isPresent();

        IdempotencyRecord secondRecord = secondRecordOpt.get();

        assertThat(secondRecord.getIdempotencyId()).isEqualTo(firstIdempotencyId);
        assertThat(secondRecord.getCreatedAt()).isEqualTo(firstCreatedAt);

        // 성공 결과 유지 확인
        assertThat(secondRecord.getPaymentId()).isEqualTo(first.paymentId());
        assertThat(secondRecord.getResponseStatus()).isEqualTo(200);

        // 현재 구현 기준: 재시도 requestId로 마지막 요청 흔적 갱신
        assertThat(secondRecord.getRequestId()).isEqualTo(requestId1);
    }
}