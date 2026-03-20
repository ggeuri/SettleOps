package com.settleops.domain.payment.application;

import com.settleops.domain.payment.api.dto.ConfirmResponseDTO;
import com.settleops.domain.payment.domain.Payment;
import com.settleops.domain.payment.domain.PaymentEventType;
import com.settleops.domain.payment.infra.PaymentEventRepository;
import com.settleops.domain.payment.infra.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test-db")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest
class ConfirmServiceIdempotencyTest {

    @Autowired
    ConfirmService confirmService;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    PaymentEventRepository paymentEventRepository;

    @Test
    @DisplayName("동일 payment confirm 재시도 시 no-op이어도 confirmedAt을 반환한다")
    void confirm_noop_should_return_confirmedAt() {
        // given: confirm 선행조건을 만족하는 CAPTURED payment
        String orderId = UUID.randomUUID().toString();
        String merchantId = "MERCHANT_1";
        String buyerId = "BUYER_1";

        Payment payment = Payment.create(orderId, merchantId, buyerId, 10_000L);
        payment.capture();
        payment = paymentRepository.saveAndFlush(payment);

        String paymentId = payment.getPaymentId();

        // when: first confirm
        String requestId1 = UUID.randomUUID().toString();
        ConfirmResponseDTO first = confirmService.confirm(paymentId, buyerId, requestId1);

        // then
        assertThat(first.confirmedAt()).isNotNull();

        // when: second confirm (no-op)
        String requestId2 = UUID.randomUUID().toString();
        ConfirmResponseDTO second = confirmService.confirm(paymentId, buyerId, requestId2);

        // then: no-op이어도 confirmedAt은 반드시 포함
        assertThat(second.confirmedAt()).isNotNull();

        // SoT인 PAYMENT_CONFIRMED.occurred_at을 그대로 반환해야 함
        assertThat(second.confirmedAt()).isEqualTo(first.confirmedAt());

        LocalDateTime sot = paymentEventRepository
                .findOccurredAtByPaymentIdAndEventType(paymentId, PaymentEventType.PAYMENT_CONFIRMED)
                .orElse(null);

        assertThat(sot).isNotNull();
        assertThat(sot).isEqualTo(first.confirmedAt());
        assertThat(sot).isEqualTo(second.confirmedAt());
    }
}