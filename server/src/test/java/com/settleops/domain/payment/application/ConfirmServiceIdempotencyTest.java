package com.settleops.domain.payment.application;

import com.settleops.domain.payment.api.dto.ConfirmResponseDTO;
import com.settleops.domain.payment.domain.Payment;
import com.settleops.domain.payment.domain.PaymentEventType;
import com.settleops.domain.payment.infra.PaymentEventRepository;
import com.settleops.domain.payment.infra.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


@ActiveProfiles("test")
@SpringBootTest
class ConfirmServiceIdempotencyTest {

    @Autowired
    ConfirmService confirmService;
    @Autowired PaymentRepository paymentRepository;
    @Autowired PaymentEventRepository paymentEventRepository;

    @Test
    void confirm_noop_should_return_confirmedAt() {
        // given: CAPTURED payment
        String orderId = UUID.randomUUID().toString();
        String merchantId = "MERCHANT_1";
        String buyerId = "BUYER_1";

        Payment payment = Payment.create(orderId, merchantId, buyerId, 10_000L);
        payment.capture(); // CAPTURED로 만들어 confirm 선행조건 충족
        payment = paymentRepository.saveAndFlush(payment);

        String paymentId = payment.getPaymentId();

        // when: first confirm (insert 성공)
        String requestId1 = UUID.randomUUID().toString();
        ConfirmResponseDTO first = confirmService.confirm(paymentId, buyerId, requestId1);

        // then: confirmedAt 존재
        assertThat(first.confirmedAt()).isNotNull();

        // when: second confirm (중복/재시도 → no-op 경로)
        String requestId2 = UUID.randomUUID().toString();
        ConfirmResponseDTO second = confirmService.confirm(paymentId, buyerId, requestId2);

        // then: no-op이어도 confirmedAt은 반드시 포함
        assertThat(second.confirmedAt()).isNotNull();

        // 그리고 SoT(occurred_at)가 동일해야 함
        assertThat(second.confirmedAt()).isEqualTo(first.confirmedAt());

        // (선택) repository SoT 조회도 null 아니어야 함
        LocalDateTime sot = paymentEventRepository
                .findOccurredAtByPaymentIdAndEventType(paymentId, PaymentEventType.PAYMENT_CONFIRMED)
                .orElse(null);
        assertThat(sot).isNotNull();
    }
}