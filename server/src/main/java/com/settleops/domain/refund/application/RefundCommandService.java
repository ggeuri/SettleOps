package com.settleops.domain.refund.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.settleops.global.enums.ReasonCode;
import com.settleops.global.error.BadRequestException;
import com.settleops.global.error.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundCommandService {

    private final RefundRepository refundRepository;
    private final RefundEventRepository refundEventRepository;
    private final PaymentRepository paymentRepository;
    private final AuditLogger auditLogger;
    private final ObjectMapper objectMapper;

    /**
     * U6: merchant 환불 요청 생성 (POST /api/refunds)
     * LOCKED 핵심:
     * - requestedAt SoT는 서비스에서 now로 세팅
     * - requestId는 Controller가 Filter 주입값을 전달하며, null/blank면 즉시 실패
     * - refund_event.request_id / audit_log.request_id는 NOT NULL 계약을 지켜야 한다
     * - audit_log.meta_json은 최소 reasonText + status diff를 남긴다
     * - payment.status가 CAPTURED가 아니면 409 RULE_VIOLATION으로 처리한다
     */
    @Transactional
    public RefundResponseDTO requestRefund(RefundCreateRequestDTO req, String requestId) {

        // 0) requestId 필수 (없으면 즉시 실패)
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalStateException("Missing requestId");
        }

        // 1) reasonText 최소 필수 (문서상 MVP 필수)
        if (req.getReasonText() == null || req.getReasonText().isBlank()) {
            throw new BadRequestException("reasonText is required");
        }

        // 2) payment 존재 + CAPTURED 가드 (LOCKED: CAPTURED일 때만 환불 요청 허용)
        // Payment 조회 예외를 400으로 변경
        Payment payment = paymentRepository.findById(req.getPaymentId())
                .orElseThrow(() -> new BadRequestException("paymentId is invalid"));

        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            // CAPTURED 가드: IllegalStateException → ConflictException(409)
            throw new ConflictException(
                    ReasonCode.PAYMENT_NOT_CAPTURED,
                    "payment is not captured"
            );
        }

        // 3) payment_id 당 refund 1회 (LOCKED/MVP)
        if (refundRepository.existsByPaymentId(req.getPaymentId())) {
            // 409 REFUND_ALREADY_EXISTS로 매핑
            throw new ConflictException(
                    ReasonCode.REFUND_ALREADY_EXISTS,
                    "refund already exists for this payment"
            );
        }

        // 3-1) refundableAmount 가드 (MVP 단순 버전)
        // MVP에서 payment_id 당 refund 1개 제한이므로 "승인합" 계산 대신,
        // 요청 금액이 capturedAmount를 넘는지만 먼저 방어
        if (req.getAmount() > payment.getCapturedAmount()) {
            throw new ConflictException(
                    ReasonCode.INSUFFICIENT_REFUNDABLE,
                    "refund amount exceeds captured amount"
            );
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
                ActorType.MERCHANT,                 // merchant 요청이면 MERCHANT
                payment.getMerchantId(),            // actor_id는 규칙대로 "짧은 id" (여기서는 merchantId 사용)
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

    private String buildMetaJsonForRequested(String reasonText) {
        try {
            Map<String, Object> meta = new HashMap<>();
            meta.put("reasonText", reasonText);

            Map<String, Object> statusDiff = new HashMap<>();
            statusDiff.put("before", null);
            statusDiff.put("after", "REQUESTED");

            Map<String, Object> diff = new HashMap<>();
            diff.put("status", statusDiff);

            meta.put("diff", diff);

            return objectMapper.writeValueAsString(meta);

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize metaJson", e);
        }
    }
}