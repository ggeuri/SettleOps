package com.settleops.domain.refund.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.settleops.domain.refund.api.dto.AdminRefundDecisionResponseDTO;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RefundAdminService {
    // 핵심 로직(approve/reject 공통):
    // - refund 조회
    // - requestId는 Controller가 Filter 주입값을 전달하며, Service는 검증만 수행
    // - 상태가 이미 APPROVED / REJECTED면 no-op 200으로 수렴
    // - no-op이어도 audit_log는 반드시 기록하고, meta_json.noOp / noOpReason을 남긴다
    // - 상태가 REQUESTED일 때만 approve(now) / reject(now) 전이 수행
    // - 응답은 status + decidedAt + requestId를 반환한다

    private final RefundRepository refundRepository;
    private final RefundEventRepository refundEventRepository;
    private final AuditLogger auditLogger;
    private final ObjectMapper objectMapper;

    /**
     * A6 운영 환불 승인.
     * - REQUESTED -> APPROVED 전이만 수행한다
     * - 이미 APPROVED / REJECTED면 no-op 200으로 응답한다
     * - no-op 여부와 사유는 audit_log.meta_json.noOp / noOpReason으로 기록한다
     */
    @Transactional
    public AdminRefundDecisionResponseDTO approve(String refundId, String adminId, String comment, String requestId) {

        requireComment(comment);
        requireRequestId(requestId);

        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new BadRequestException("refundId is invalid"));

        // [필수] no-op 200 수렴(이미 결정됨)
        if (refund.getStatus() == RefundStatus.APPROVED || refund.getStatus() == RefundStatus.REJECTED) {
            requireDecidedAtIfDecided(refund);   // decidedAt 정합 깨졌으면 빨리 터뜨리기

            String noOpReason = getRefundNoOpReason(refund.getStatus());

            auditLogger.log(AuditLogCommand.builder()
                    .requestId(requestId)
                    .actorType(ActorType.ADMIN)
                    .actorId(adminId)
                    .action(Action.REFUND_APPROVED)
                    .entityType(EntityType.REFUND)
                    .entityId(refund.getRefundId())
                    .statusBefore(refund.getStatus().name())
                    .statusAfter(refund.getStatus().name())
                    .merchantId(refund.getMerchantId())
                    .metaJson(buildMetaJson(
                            comment,
                            refund.getStatus().name(),
                            refund.getStatus().name(),
                            true,
                            noOpReason
                    ))
                    .build());
            return response(refund, requestId);  // 여기서 종료
        }

        // REQUESTED에서만 전이
        LocalDateTime now = LocalDateTime.now();
        RefundStatus before = refund.getStatus();
        refund.approve(now);

        // insert-only refund_event
        RefundEvent event = RefundEventFactory.approved(
                refund.getRefundId(),
                before,
                requestId,
                ActorType.ADMIN,
                adminId,
                null
        );
        refundEventRepository.save(event);

        // audit_log (comment는 meta_json.comment에)
        auditLogger.log(AuditLogCommand.builder()
                .requestId(requestId)
                .actorType(ActorType.ADMIN)
                .actorId(adminId)
                .action(Action.REFUND_APPROVED)
                .entityType(EntityType.REFUND)
                .entityId(refund.getRefundId())
                .statusBefore(before.name())
                .statusAfter(refund.getStatus().name())
                .merchantId(refund.getMerchantId())
                .metaJson(buildMetaJson(
                        comment,
                        before.name(),
                        refund.getStatus().name(),
                        false,
                        null
                ))
                .build());

        requireDecidedAtIfDecided(refund);
        return response(refund, requestId);
    }

    /**
     * A6 운영 환불 거절.
     * - REQUESTED -> REJECTED 전이만 수행한다
     * - 이미 APPROVED / REJECTED면 no-op 200으로 응답한다
     * - no-op 여부와 사유는 audit_log.meta_json.noOp / noOpReason으로 기록한다
     */
    @Transactional
    public AdminRefundDecisionResponseDTO reject(String refundId, String adminId, String comment, String requestId) {

        requireComment(comment);
        requireRequestId(requestId);

        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new BadRequestException("refundId is invalid"));

        // [필수] no-op 200 수렴
        if (refund.getStatus() == RefundStatus.APPROVED || refund.getStatus() == RefundStatus.REJECTED) {
            requireDecidedAtIfDecided(refund);

            String noOpReason = getRefundNoOpReason(refund.getStatus());

            auditLogger.log(AuditLogCommand.builder()
                    .requestId(requestId)
                    .actorType(ActorType.ADMIN)
                    .actorId(adminId)
                    .action(Action.REFUND_REJECTED)
                    .entityType(EntityType.REFUND)
                    .entityId(refund.getRefundId())
                    .statusBefore(refund.getStatus().name())
                    .statusAfter(refund.getStatus().name())
                    .merchantId(refund.getMerchantId())
                    .metaJson(buildMetaJson(
                            comment,
                            refund.getStatus().name(),
                            refund.getStatus().name(),
                            true,
                            noOpReason
                    ))
                    .build());

            return response(refund, requestId);
        }

        LocalDateTime now = LocalDateTime.now();
        RefundStatus before = refund.getStatus();
        refund.reject(now);

        RefundEvent event = RefundEventFactory.rejected(
                refund.getRefundId(),
                before,
                requestId,
                ActorType.ADMIN,
                adminId,
                null
        );
        refundEventRepository.save(event);

        auditLogger.log(AuditLogCommand.builder()
                .requestId(requestId)
                .actorType(ActorType.ADMIN)
                .actorId(adminId)
                .action(Action.REFUND_REJECTED)
                .entityType(EntityType.REFUND)
                .entityId(refund.getRefundId())
                .statusBefore(before.name())
                .statusAfter(refund.getStatus().name())
                .merchantId(refund.getMerchantId())
                .metaJson(buildMetaJson(
                        comment,
                        before.name(),
                        refund.getStatus().name(),
                        false,
                        null
                ))                .build());

        requireDecidedAtIfDecided(refund);
        return response(refund, requestId);
    }

    // comment 필수 요청 가드
    private void requireComment(String comment) {
        if (comment == null || comment.isBlank()) {
            throw new BadRequestException("comment is required");// 400 고정
            // 또는 커스텀 BadRequest 예외로 400 매핑
        }
    }

    // 조회가 아니라 검증만
    private void requireRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalStateException("Missing requestId");
        }
    }

    // decidedAt null 점검
    private void requireDecidedAtIfDecided(Refund refund) {
        if ((refund.getStatus() == RefundStatus.APPROVED || refund.getStatus() == RefundStatus.REJECTED)
                && refund.getDecidedAt() == null) {
            throw new IllegalStateException("decidedAt must exist when refund is decided");
        }
    }

    private AdminRefundDecisionResponseDTO response(Refund refund, String requestId) {
        // [필수] status + decidedAt + requestId (no-op 포함)
        return AdminRefundDecisionResponseDTO.builder()
                .status(refund.getStatus())
                .decidedAt(refund.getDecidedAt())
                .requestId(requestId)
                .build();
    }

    private String buildMetaJson(String comment, String before, String after, boolean noOp, String noOpReason) {
        try {
            Map<String, Object> meta = new HashMap<>();

            meta.put("comment", comment);
            meta.put("noOp", noOp);

            if (noOp) {
                meta.put("noOpReason", noOpReason);
            }

            Map<String, Object> statusDiff = new HashMap<>();
            statusDiff.put("before", before);
            statusDiff.put("after", after);

            Map<String, Object> diff = new HashMap<>();
            diff.put("status", statusDiff);

            meta.put("diff", diff);

            return objectMapper.writeValueAsString(meta);

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize metaJson", e);
        }
    }

    private String getRefundNoOpReason(RefundStatus status) {
        return switch (status) {
            case APPROVED -> "ALREADY_APPROVED";
            case REJECTED -> "ALREADY_REJECTED";
            default -> throw new IllegalArgumentException("No noOpReason for status: " + status);
        };
    }
}
