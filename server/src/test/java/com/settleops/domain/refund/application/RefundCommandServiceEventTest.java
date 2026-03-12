package com.settleops.domain.refund.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.settleops.domain.payment.domain.Payment;
import com.settleops.domain.payment.domain.PaymentStatus;
import com.settleops.domain.payment.infra.PaymentRepository;
import com.settleops.domain.refund.api.dto.RefundCreateRequestDTO;
import com.settleops.domain.refund.domain.RefundEvent;
import com.settleops.domain.refund.domain.RefundEventType;
import com.settleops.domain.refund.domain.RefundStatus;
import com.settleops.domain.refund.infra.RefundEventRepository;
import com.settleops.domain.refund.infra.RefundRepository;
import com.settleops.global.audit.ActorType;
import com.settleops.global.audit.AuditLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Day 4 - RefundCommandService 이벤트 insert-only 보장 테스트
 * 핵심 검증:
 *   1. 환불 요청 → refund_event 정확히 1건 저장
 *   2. 저장된 이벤트 필드(eventType, statusBefore, statusAfter, requestId, actorType)
 *   3. 룰 가드 실패(PAYMENT_NOT_CAPTURED, REFUND_ALREADY_EXISTS, INSUFFICIENT_REFUNDABLE)에서는 이벤트 저장 없음
 */
@ExtendWith(MockitoExtension.class)
class RefundCommandServiceEventTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private RefundRepository refundRepository;
    @Mock private RefundEventRepository refundEventRepository;
    @Mock private AuditLogger auditLogger;
    // ObjectMapper는 실제 인스턴스로 주입 (직렬화 로직 실제 검증)
    @Spy  private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private RefundCommandService refundCommandService;

    @Test
    @DisplayName("환불 요청 → refund_event 1건만 적재 (REFUND_REQUESTED)")
    void requestRefund_saves_exactly_one_event() {
        // given
        String requestId = "req-" + UUID.randomUUID();
        Payment payment = buildCapturedPaymentWithMerchant("merchant-001");

        when(paymentRepository.findById("pay-001")).thenReturn(Optional.of(payment));
        when(refundRepository.existsByPaymentId("pay-001")).thenReturn(false);

        RefundCreateRequestDTO req = RefundCreateRequestDTO.builder()
                .paymentId("pay-001")
                .amount(10_000L)
                .reasonText("단순변심")
                .build();

        ArgumentCaptor<RefundEvent> eventCaptor = ArgumentCaptor.forClass(RefundEvent.class);

        // when
        refundCommandService.requestRefund(req, requestId);

        // then: 정확히 1건만 save
        verify(refundRepository, times(1)).save(any());
        verify(refundEventRepository, times(1)).save(eventCaptor.capture());
        verifyNoMoreInteractions(refundEventRepository);
        RefundEvent saved = eventCaptor.getValue();

        // LOCKED: REFUND_REQUESTED 이벤트 필드 계약
        assertThat(saved.getEventType()).isEqualTo(RefundEventType.REFUND_REQUESTED);
        assertThat(saved.getStatusBefore()).isNull(); // REQUESTED는 before=null
        assertThat(saved.getStatusAfter()).isEqualTo(RefundStatus.REQUESTED);
        assertThat(saved.getRequestId()).isEqualTo(requestId); // requestId NOT NULL
        assertThat(saved.getActorType()).isEqualTo(ActorType.MERCHANT);
        assertThat(saved.getActorId()).isEqualTo("merchant-001");
    }

    @Test
    @DisplayName("PAYMENT_NOT_CAPTURED → 409, 이벤트 저장 없음")
    void requestRefund_payment_not_captured_no_event_saved() {

        // Payment.builder() 대신 mock으로 대체
        Payment payment = mock(Payment.class);
        when(payment.getStatus()).thenReturn(PaymentStatus.CREATED); // CAPTURED 아님

        when(paymentRepository.findById("pay-001")).thenReturn(Optional.of(payment));

        assertThatThrownBy(() ->
                refundCommandService.requestRefund(
                        RefundCreateRequestDTO.builder()
                                .paymentId("pay-001").amount(1_000L).reasonText("사유").build(),
                        "req-001"
                )
        ).isInstanceOf(com.settleops.global.error.ConflictException.class);

        verifyNoInteractions(refundEventRepository);
        verify(refundRepository, never()).save(any());
    }

    @Test
    @DisplayName("REFUND_ALREADY_EXISTS → 409, 이벤트 저장 없음")
    void requestRefund_already_exists_no_event_saved() {
        // given
        Payment payment = mock(Payment.class);
        when(payment.getStatus()).thenReturn(PaymentStatus.CAPTURED);

        when(paymentRepository.findById("pay-001")).thenReturn(Optional.of(payment));
        when(refundRepository.existsByPaymentId("pay-001")).thenReturn(true);

        // when/then
        assertThatThrownBy(() ->
                refundCommandService.requestRefund(
                        RefundCreateRequestDTO.builder()
                                .paymentId("pay-001").amount(1_000L).reasonText("사유").build(),
                        "req-001"
                )
        ).isInstanceOf(com.settleops.global.error.ConflictException.class);

        verify(refundRepository, never()).save(any());
        verifyNoInteractions(refundEventRepository);
    }

    @Test
    @DisplayName("INSUFFICIENT_REFUNDABLE → 409, 이벤트 저장 없음")
    void requestRefund_exceeds_captured_amount_no_event_saved() {
        // given: capturedAmount = 10,000인데 요청 금액은 20,000 → 초과라 409 발생해야 함

        // Payment.builder() 대신 mock으로 생성 (@Builder 없으니까)
        Payment cheapPayment = mock(Payment.class);
        when(cheapPayment.getStatus()).thenReturn(PaymentStatus.CAPTURED); // CAPTURED는 맞음
        when(cheapPayment.getCapturedAmount()).thenReturn(10_000L);        // 근데 10,000만 가능

        when(paymentRepository.findById("pay-001")).thenReturn(Optional.of(cheapPayment));
        when(refundRepository.existsByPaymentId("pay-001")).thenReturn(false);

        // when/then: 20,000 요청 → INSUFFICIENT_REFUNDABLE 409
        assertThatThrownBy(() ->
                refundCommandService.requestRefund(
                        RefundCreateRequestDTO.builder()
                                .paymentId("pay-001").amount(20_000L).reasonText("사유").build(),
                        "req-001"
                )
        ).isInstanceOf(com.settleops.global.error.ConflictException.class);

        // 가드에서 막혔으니 이벤트 저장은 0건이어야 함
        verify(refundRepository, never()).save(any());
        verifyNoInteractions(refundEventRepository);
    }

    // ---- helper ----
    private Payment buildCapturedPaymentWithMerchant(String merchantId) {
        Payment payment = mock(Payment.class);
        when(payment.getStatus()).thenReturn(PaymentStatus.CAPTURED);
        when(payment.getCapturedAmount()).thenReturn(50_000L);
        when(payment.getMerchantId()).thenReturn(merchantId);
        return payment;
    }
}
