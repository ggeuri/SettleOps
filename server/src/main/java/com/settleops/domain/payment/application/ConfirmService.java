package com.settleops.domain.payment.application;

import com.settleops.domain.order.application.OrderService;
import com.settleops.domain.payment.api.dto.PayResponseDTO;
import com.settleops.domain.payment.domain.Payment;
import com.settleops.domain.payment.domain.PaymentEvent;
import com.settleops.domain.payment.domain.PaymentEventType;
import com.settleops.domain.payment.domain.PaymentStatus;
import com.settleops.domain.payment.infra.PaymentEventRepository;
import com.settleops.domain.payment.infra.PaymentRepository;
import com.settleops.global.error.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ConfirmService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final OrderService orderService;

    /**
     * Payment Confirm 유스케이스 (SoT = PAYMENT_CONFIRMED 이벤트)
     * - payment.status는 CAPTURED 유지 (CONFIRMED는 이벤트로만 표현)
     * - 멱등: PAYMENT_CONFIRMED 이벤트 중복 insert 방지
     */
    public PayResponseDTO confirm(String paymentId) {

        // 1) Payment 조회
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BadRequestException("존재하지 않는 결제입니다."));

        // 2) 선행조건 검증: CAPTURED에서만 confirm 허용
        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            throw new BadRequestException("CAPTURED 상태에서만 CONFIRM 가능합니다.");
        }

        // 3) 멱등 + 경쟁조건 처리
        // - 가장 안전한 방식: (payment_id, event_type) 유니크 제약 + insert 시도
        // - 중복키면 이미 CONFIRMED로 간주하고 그대로 응답
        try {
            paymentEventRepository.save(PaymentEvent.confirmed(payment.getPaymentId()));
        } catch (DataIntegrityViolationException e) {
            // 이미 PAYMENT_CONFIRMED 이벤트가 존재하는 경우(동시 요청/재시도)
            return buildResponse(payment);
        }

        // 4) 응답
        return buildResponse(payment);
    }

    /**
     * Confirm 응답 DTO 생성
     * - confirmedAt은 PAYMENT_CONFIRMED.occurred_at을 사용
     */
    private PayResponseDTO buildResponse(Payment payment) {

        LocalDateTime confirmedAt = paymentEventRepository
                .findOccurredAtByPaymentIdAndEventType(
                        payment.getPaymentId(),
                        PaymentEventType.PAYMENT_CONFIRMED
                )
                .orElseThrow(() -> new IllegalStateException("CONFIRMED 이벤트가 존재하지 않습니다."));

        return PayResponseDTO.from(payment, confirmedAt);
    }
}