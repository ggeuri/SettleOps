package com.settleops.domain.payment.application;

import com.settleops.domain.payment.api.dto.PayResponseDTO;
import com.settleops.domain.payment.domain.Payment;
import com.settleops.domain.payment.domain.PaymentEvent;
import com.settleops.domain.payment.domain.PaymentEventType;
import com.settleops.domain.payment.domain.PaymentStatus;
import com.settleops.domain.payment.infra.PaymentEventRepository;
import com.settleops.domain.payment.infra.PaymentRepository;
import com.settleops.global.audit.ActorType;
import com.settleops.global.audit.AuditLogCommand;
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.audit.EntityType;
import com.settleops.global.db.DbConstraintUtils;
import com.settleops.global.enums.Action;
import com.settleops.global.enums.ReasonCode;
import com.settleops.global.error.BadRequestException;
import com.settleops.global.error.ConflictException;
import com.settleops.global.error.ForbiddenException;
import com.settleops.global.logging.RequestIdProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ConfirmService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final AuditLogger auditLogger;

    /**
     * <p>Payment Confirm 유스케이스 (SoT = PAYMENT_CONFIRMED 이벤트)<br/>
     * - payment.status는 CAPTURED 유지 (CONFIRMED는 이벤트로만 표현)<br/>
     * - 멱등: PAYMENT_CONFIRMED 이벤트 중복 insert 방지</p>
     */
    @Transactional
    public PayResponseDTO confirm(String paymentId, String currentBuyerId) {

        // 1) Payment 조회
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BadRequestException("존재하지 않는 결제입니다."));

        // 2) 선행조건 검증: CAPTURED에서만 confirm 허용 _ 불일치 시 409
        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            throw new ConflictException(ReasonCode.PAYMENT_NOT_CAPTURED,"CAPTURED 상태에서만 CONFIRM 가능합니다.");
        }

        // 인증 주체 없음 → 403 UNAUTHORIZED
        if (currentBuyerId == null || currentBuyerId.isBlank()) {
            throw new ForbiddenException("UNAUTHORIZED");
        }

        String buyerId = payment.getBuyerId();

        // payment.buyer_id 누락은 내부 정합성 오류(500)로 처리(운영자만 보는 오류)
        if (buyerId == null || buyerId.isBlank()) {
            throw new IllegalStateException("payment.buyer_id is null/blank");
        }

        // buyer 불일치 → 403 Forbidden (reason 사용 안함 정책)
        if (!buyerId.equals(currentBuyerId)) throw new ForbiddenException("BUYER_MISMATCH");

        final String requestId = RequestIdProvider.current();

        // 3) 멱등 + 경쟁조건 처리
        // - 가장 안전한 방식: (payment_id, event_type) 유니크 제약 + insert 시도
        // - 중복키면 이미 CONFIRMED로 간주하고 그대로 응답
        try {
            // (payment_id, event_type) UNIQUE 기반 멱등 처리
            // flush로 confirmed 이벤트를 DB에 즉시 반영하여, 아래 occurred_at 조회가 항상 성공하도록 보장
            paymentEventRepository.saveAndFlush(PaymentEvent.confirmed(paymentId));

            LocalDateTime confirmedAt = getConfirmedAtOrThrow(paymentId);

            // ✅ 최초 confirm 성공 로그 (idempotent=false)
            // - "요청이 실제로 상태(=이벤트 SoT)를 전진시켰다"는 행위 로그
            // - payment.status는 CAPTURED 유지(LOCKED) → before/after 동일
            auditLogger.log(
                    AuditLogCommand.builder()
                            .requestId(requestId)
                            .action(Action.PAYMENT_CONFIRMED)
                            .actorType(ActorType.BUYER)
                            .actorId(currentBuyerId)
                            .entityType(EntityType.PAYMENT)
                            .entityId(paymentId)
                            .statusBefore(PaymentStatus.CAPTURED.name())
                            .statusAfter(PaymentStatus.CAPTURED.name())
                            .merchantId(payment.getMerchantId())
                            .occurredAt(confirmedAt)
                            // 프런트에서 no-op 필터링 가능한 키 제공
                            .metaJson("{\"idempotent\":false}")
                            .build()
            );

            return PayResponseDTO.from(payment, confirmedAt);

        } catch (DataIntegrityViolationException e) {

            // 동시 요청/재시도: 이미 PAYMENT_CONFIRMED 이벤트가 존재하는 경우
            // - duplicate key일 때만 "이미 CONFIRMED"로 확정할 수 있으므로 여기서만 no-op 처리
            if (DbConstraintUtils.isDuplicateKey(e)) {

                LocalDateTime confirmedAt = getConfirmedAtOrThrow(paymentId);

                // ✅ no-op confirm 로그 (idempotent=true)
                // - "요청은 들어왔고, 결과는 멱등(no-op)으로 수렴"을 행위 로그로 남김
                // - 프런트는 metaJson.idempotent=true를 기준으로 숨김 처리 가능
                auditLogger.log(
                        AuditLogCommand.builder()
                                .requestId(requestId)
                                .action(Action.PAYMENT_CONFIRMED)
                                .actorType(ActorType.BUYER)
                                .actorId(currentBuyerId)
                                .entityType(EntityType.PAYMENT)
                                .entityId(paymentId)
                                .statusBefore(PaymentStatus.CAPTURED.name())
                                .statusAfter(PaymentStatus.CAPTURED.name())
                                .merchantId(payment.getMerchantId())
                                .occurredAt(confirmedAt)
                                .metaJson("{\"idempotent\":true}")
                                .build()
                );

                // no-op도 동일 응답 반환(confirmedAt 포함)
                return PayResponseDTO.from(payment, confirmedAt);
            }

            // duplicate가 아니면 DB/시스템 오류 (500)
            throw e;
        }
    }

    /**
     * confirmedAt 조회 (SoT = PAYMENT_CONFIRMED.occurred_at)
     */
    private LocalDateTime getConfirmedAtOrThrow(String paymentId) {
        return paymentEventRepository
                .findOccurredAtByPaymentIdAndEventType(paymentId, PaymentEventType.PAYMENT_CONFIRMED)
                .orElseThrow(() -> new IllegalStateException("CONFIRMED 이벤트가 존재하지 않습니다. paymentId=" + paymentId));
    }
}