package com.settleops.domain.refund.application;

import com.settleops.domain.payment.domain.Payment;
import com.settleops.domain.payment.domain.PaymentStatus;
import com.settleops.domain.payment.infra.PaymentRepository;
import com.settleops.domain.refund.api.dto.RefundCreateRequestDTO;
import com.settleops.domain.refund.api.dto.RefundResponseDTO;
import com.settleops.domain.refund.domain.Refund;
import com.settleops.domain.refund.domain.RefundEvent;
import com.settleops.domain.refund.domain.RefundEventFactory;
import com.settleops.domain.refund.domain.RefundStatus;
import com.settleops.domain.refund.infra.RefundEventRepository;
import com.settleops.domain.refund.infra.RefundRepository;
import com.settleops.global.audit.ActorType;
import com.settleops.global.audit.AuditLogCommand;
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.audit.EntityType;
import com.settleops.global.enums.Action;
import com.settleops.global.error.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundCommandService {

    private final RefundRepository refundRepository;
    private final RefundEventRepository refundEventRepository;
    private final PaymentRepository paymentRepository;
    private final AuditLogger auditLogger;

    /**
     * U6: merchant 환불 요청 생성 (POST /api/refunds)
     * LOCKED 핵심:
     * - requestedAt SoT는 서비스에서 now로 세팅
     * - refund_event.request_id NOT NULL 강제 (MDC 없으면 실패)
     * - audit_log.meta_json은 최소 reason + status diff
     * - payment CAPTURED 아니면 409(룰 위반)로 처리해야 하지만,
     *   지금은 최소 코드라 일단 IllegalStateException/커스텀 예외로 던지고
     *   GlobalExceptionHandler에서 409로 매핑해주면 됨
     */
    @Transactional
    public RefundResponseDTO requestRefund(RefundCreateRequestDTO req) {

        // 0) requestId 필수 (없으면 즉시 실패)
        String requestId = currentRequestId();

        // 1) reasonText 최소 필수 (문서상 MVP 필수)
        if (req.getReasonText() == null || req.getReasonText().isBlank()) {
            throw new BadRequestException("reasonText is required");
        }

        // 2) payment 존재 + CAPTURED 가드 (LOCKED: CAPTURED일 때만 환불 요청 허용)
        Payment payment = paymentRepository.findById(req.getPaymentId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + req.getPaymentId()));

        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            // ✅ 여기서 409 PAYMENT_NOT_CAPTURED로 매핑되게 예외 타입을 맞추는 게 이상적
            throw new IllegalStateException("PAYMENT_NOT_CAPTURED");
        }

        // 3) payment_id 당 refund 1회 (LOCKED/MVP)
        if (refundRepository.existsByPaymentId(req.getPaymentId())) {
            // ✅ 여기서 409 REFUND_ALREADY_EXISTS로 매핑
            throw new IllegalStateException("REFUND_ALREADY_EXISTS");
        }

        // 4) requestedAt SoT = "업무 요청 시각" (서비스에서 now)
        LocalDateTime now = LocalDateTime.now();

        // 5) refund row 생성/저장
        String refundId = UUID.randomUUID().toString();
        Refund refund = Refund.builder()
                .refundId(refundId)
                .paymentId(req.getPaymentId())
                .merchantId(payment.getMerchantId()) // 스냅샷
                .buyerId(payment.getBuyerId())       // 스냅샷
                .amount(req.getAmount())
                .currency(payment.getCurrency())
                .status(RefundStatus.REQUESTED)
                .reasonText(req.getReasonText())
                .requestedAt(now)
                .decidedAt(null)
                .build();

        refundRepository.save(refund);

        // 6) refund_event (insert-only) - request_id NOT NULL 강제
        RefundEvent event = RefundEventFactory.requested(
                refundId,
                requestId,
                ActorType.MERCHANT,                 // ✅ merchant 요청이면 MERCHANT
                payment.getMerchantId(),            // ✅ actor_id는 규칙대로 "짧은 id" (여기서는 merchantId 사용)
                null                                // occurredAt은 @PrePersist가 채움
        );
        refundEventRepository.save(event);

        // 7) audit_log (insert-only) - meta_json에 reason + status diff
        auditLogger.log(AuditLogCommand.builder()
                .requestId(requestId)
                .actorType(ActorType.MERCHANT)
                .actorId(payment.getMerchantId())
                .action(Action.REFUND_REQUESTED)
                .entityType(EntityType.REFUND)
                .entityId(refundId)
                .statusBefore(null)
                .statusAfter(RefundStatus.REQUESTED.name())
                .merchantId(payment.getMerchantId())
                .metaJson(buildMetaJsonForRequested(req.getReasonText()))
                .build());

        // 8) 응답
        return RefundResponseDTO.builder()
                .refundId(refundId)
                .paymentId(req.getPaymentId())
                .amount(req.getAmount())
                .status(RefundStatus.REQUESTED.name())
                .requestedAt(now)
                .build();
    }

    private String currentRequestId() {
        String requestId = MDC.get("requestId");
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalStateException("Missing requestId in MDC");
        }
        return requestId;
    }

    private String buildMetaJsonForRequested(String reasonText) {
        return "{"
                + "\"reasonText\":" + toJsonString(reasonText) + ","
                + "\"diff\":{\"status\":{\"before\":null,\"after\":\"REQUESTED\"}}"
                + "}";
    }

    private String toJsonString(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}