package com.settleops.domain.payment.application;

import com.settleops.domain.payment.api.dto.ConfirmResponseDTO;
import com.settleops.domain.payment.domain.Payment;
import com.settleops.domain.payment.domain.PaymentEventType;
import com.settleops.domain.payment.infra.PaymentEventRepository;
import com.settleops.domain.payment.infra.PaymentRepository;
import com.settleops.global.error.ConflictException;
import com.settleops.global.error.ForbiddenException;
import com.settleops.global.error.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test-db")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest
class ConfirmServiceTest {

    private static final String MERCHANT_ID = "MERCHANT_1";
    private static final String BUYER_ID = "BUYER_1";
    private static final String OTHER_BUYER_ID = "BUYER_2";

    @Autowired
    private ConfirmService confirmService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentEventRepository paymentEventRepository;

    @Test
    @DisplayName("confirm 최초 호출 시 PAYMENT_CONFIRMED 이벤트가 생성되고 confirmedAt을 반환한다")
    void confirm_success_should_return_confirmedAt() {
        // given
        Payment payment = createCapturedPayment(MERCHANT_ID, BUYER_ID, 10_000L);
        String paymentId = payment.getPaymentId();
        String orderId = payment.getOrderId();
        String requestId = UUID.randomUUID().toString();

        // when
        ConfirmResponseDTO result = confirmService.confirm(paymentId, BUYER_ID, requestId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.paymentId()).isEqualTo(paymentId);
        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.status()).isEqualTo("CAPTURED");
        assertThat(result.confirmedAt()).isNotNull();

        LocalDateTime confirmedAt = paymentEventRepository
                .findOccurredAtByPaymentIdAndEventType(paymentId, PaymentEventType.PAYMENT_CONFIRMED)
                .orElse(null);

        assertThat(confirmedAt).isNotNull();
        assertThat(result.confirmedAt()).isEqualTo(confirmedAt);
    }

    @Test
    @DisplayName("동일 payment 재confirm 시 no-op이어도 기존 confirmedAt을 그대로 반환한다")
    void confirm_noop_should_return_existing_confirmedAt() {
        // given
        Payment payment = createCapturedPayment(MERCHANT_ID, BUYER_ID, 10_000L);
        String paymentId = payment.getPaymentId();
        String orderId = payment.getOrderId();

        String requestId1 = UUID.randomUUID().toString();
        String requestId2 = UUID.randomUUID().toString();

        // when
        ConfirmResponseDTO first = confirmService.confirm(paymentId, BUYER_ID, requestId1);
        ConfirmResponseDTO second = confirmService.confirm(paymentId, BUYER_ID, requestId2);

        // then
        assertThat(first.paymentId()).isEqualTo(paymentId);
        assertThat(first.orderId()).isEqualTo(orderId);
        assertThat(first.status()).isEqualTo("CAPTURED");
        assertThat(first.confirmedAt()).isNotNull();

        assertThat(second.paymentId()).isEqualTo(paymentId);
        assertThat(second.orderId()).isEqualTo(orderId);
        assertThat(second.status()).isEqualTo("CAPTURED");
        assertThat(second.confirmedAt()).isNotNull();
        assertThat(second.confirmedAt()).isEqualTo(first.confirmedAt());

        LocalDateTime confirmedAt = paymentEventRepository
                .findOccurredAtByPaymentIdAndEventType(paymentId, PaymentEventType.PAYMENT_CONFIRMED)
                .orElse(null);

        assertThat(confirmedAt).isNotNull();
        assertThat(confirmedAt).isEqualTo(first.confirmedAt());
        assertThat(confirmedAt).isEqualTo(second.confirmedAt());
    }

    @Test
    @DisplayName("재confirm 되어도 PAYMENT_CONFIRMED 이벤트는 1건만 유지된다")
    void confirm_noop_should_not_create_duplicate_confirmed_event() {
        // given
        Payment payment = createCapturedPayment(MERCHANT_ID, BUYER_ID, 10_000L);
        String paymentId = payment.getPaymentId();

        String requestId1 = UUID.randomUUID().toString();
        String requestId2 = UUID.randomUUID().toString();

        // when
        ConfirmResponseDTO first = confirmService.confirm(paymentId, BUYER_ID, requestId1);
        ConfirmResponseDTO second = confirmService.confirm(paymentId, BUYER_ID, requestId2);

        // then
        assertThat(first.confirmedAt()).isNotNull();
        assertThat(second.confirmedAt()).isEqualTo(first.confirmedAt());

        long confirmedEventCount = paymentEventRepository.countByPaymentIdAndEventType(
                paymentId,
                PaymentEventType.PAYMENT_CONFIRMED
        );

        assertThat(confirmedEventCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 paymentId로 confirm 호출 시 NotFoundException이 발생한다")
    void confirm_should_throw_not_found_when_payment_not_exists() {
        // given
        String notExistingPaymentId = UUID.randomUUID().toString();
        String requestId = UUID.randomUUID().toString();

        // when & then
        assertThatThrownBy(() -> confirmService.confirm(notExistingPaymentId, BUYER_ID, requestId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("buyer 불일치 시 ForbiddenException이 발생한다")
    void confirm_should_throw_forbidden_when_buyer_mismatch() {
        // given
        Payment payment = createCapturedPayment(MERCHANT_ID, BUYER_ID, 10_000L);
        String paymentId = payment.getPaymentId();
        String requestId = UUID.randomUUID().toString();

        // when & then
        assertThatThrownBy(() -> confirmService.confirm(paymentId, OTHER_BUYER_ID, requestId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("CAPTURED 상태가 아니면 ConflictException이 발생한다")
    void confirm_should_throw_conflict_when_payment_not_captured() {
        // given
        Payment payment = Payment.create(
                UUID.randomUUID().toString(),
                MERCHANT_ID,
                BUYER_ID,
                10_000L
        );
        payment = paymentRepository.saveAndFlush(payment);

        String paymentId = payment.getPaymentId();
        String requestId = UUID.randomUUID().toString();

        // when & then
        assertThatThrownBy(() -> confirmService.confirm(paymentId, BUYER_ID, requestId))
                .isInstanceOf(ConflictException.class);
    }

    private Payment createCapturedPayment(String merchantId, String buyerId, long amount) {
        Payment payment = Payment.create(
                UUID.randomUUID().toString(),
                merchantId,
                buyerId,
                amount
        );
        payment.capture();
        return paymentRepository.saveAndFlush(payment);
    }
}