package com.settleops.domain.payment.application;

import com.settleops.domain.payment.api.dto.PayResponseDTO;
import com.settleops.domain.payment.domain.Payment;
import com.settleops.domain.payment.domain.PaymentEvent;
import com.settleops.domain.payment.domain.PaymentEventType;
import com.settleops.domain.payment.domain.PaymentStatus;
import com.settleops.domain.payment.infra.PaymentEventRepository;
import com.settleops.domain.payment.infra.PaymentRepository;
import com.settleops.global.db.DbConstraintUtils;
import com.settleops.global.enums.ReasonCode;
import com.settleops.global.error.BadRequestException;
import com.settleops.global.error.ConflictException;
import com.settleops.global.error.ForbiddenException;
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

    /**
     * <p>Payment Confirm 유스케이스 (SoT = PAYMENT_CONFIRMED 이벤트)<br/>
     * - payment.status는 CAPTURED 유지 (CONFIRMED는 이벤트로만 표현)<br/>
     * - 멱등: PAYMENT_CONFIRMED 이벤트 중복 insert 방지</p>
     */
    public PayResponseDTO confirm(String paymentId, String currentBuyerId) {

        // 1) Payment 조회
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BadRequestException("존재하지 않는 결제입니다."));

        // 2) 선행조건 검증: CAPTURED에서만 confirm 허용 _ 불일치 시 409
        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            throw new ConflictException(ReasonCode.PAYMENT_NOT_CAPTURED,"CAPTURED 상태에서만 CONFIRM 가능합니다.");
        }

        String buyerId = payment.getBuyerId();

        // payment.buyer_id 누락은 내부 정합성 오류(500)로 처리(운영자만 보는 오류)
        if (buyerId == null || buyerId.isBlank()) {
            throw new IllegalStateException("payment.buyer_id is null/blank");
        }

        // buyer 불일치 → 403 Forbidden (reason 사용 안함 정책)
        if (!buyerId.equals(currentBuyerId)) throw new ForbiddenException("BUYER_MISMATCH");

        // 3) 멱등 + 경쟁조건 처리
        // - 가장 안전한 방식: (payment_id, event_type) 유니크 제약 + insert 시도
        // - 중복키면 이미 CONFIRMED로 간주하고 그대로 응답
        try {
            // (payment_id, event_type) UNIQUE 기반 멱등 처리
            // flush로 confirmed 이벤트를 DB에 확정시킨 뒤, occurred_at 조회가 항상 성공하도록 보장
            paymentEventRepository.saveAndFlush(PaymentEvent.confirmed(paymentId));

        } catch (DataIntegrityViolationException e) {
            // 동시 요청/재시도: 이미 CONFIRMED 이벤트 존재 → 멱등 응답
            if (DbConstraintUtils.isDuplicateKey(e)) {
                return buildResponse(payment);
            }
            throw e;
        }

        // 4) 응답
        return buildResponse(payment);
    }

    /**
     * <p>Confirm 응답 DTO 생성<br/>
     * - confirmedAt은 PAYMENT_CONFIRMED.occurred_at을 사용</p>
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