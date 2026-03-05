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
import com.settleops.global.logging.RequestIdKeys;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RefundAdminService {
    //핵심 로직(approve 예시):
    //refund 조회
    //상태가 이미 APPROVED면 no-op 200: 기존 decidedAt 그대로 반환
    //상태가 이미 REJECTED면 팀 정책에 따라
    //피드백 문장 그대로 해석하면 no-op 200 수렴이므로 no-op으로 맞추는 게 안전합니다.
    //상태가 REQUESTED일 때만 refund.approve(now) 호출
    //응답은 반드시 status + decidedAt를 포함합니다.

    private final RefundRepository refundRepository;
    private final RefundEventRepository refundEventRepository;
    private final AuditLogger auditLogger;
    private final ObjectMapper objectMapper;

    @Transactional
    public AdminRefundDecisionResponseDTO approve(String refundId, String adminId, String comment) {

        requireComment(comment);

        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new BadRequestException("refundId is invalid"));

        String requestId = currentRequestId(); // [필수] requestId null/blank면 즉시 실패

        // [필수] no-op 200 수렴(이미 결정됨)
        if (refund.getStatus() == RefundStatus.APPROVED || refund.getStatus() == RefundStatus.REJECTED) {
            requireDecidedAtIfDecided(refund);   // decidedAt 정합 깨졌으면 빨리 터뜨리기
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
                .metaJson(buildMetaJson(comment, before.name(), refund.getStatus().name()))
                .build());

        requireDecidedAtIfDecided(refund);
        return response(refund, requestId);
    }

    @Transactional
    public AdminRefundDecisionResponseDTO reject(String refundId, String adminId, String comment) {

        requireComment(comment);

        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found: " + refundId));

        String requestId = currentRequestId();

        // [필수] no-op 200 수렴
        if (refund.getStatus() == RefundStatus.APPROVED || refund.getStatus() == RefundStatus.REJECTED) {
            requireDecidedAtIfDecided(refund);
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
                .metaJson(buildMetaJson(comment, before.name(), refund.getStatus().name()))
                .build());

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

    private String currentRequestId() {
        String requestId = MDC.get(RequestIdKeys.MDC_KEY);
        if (requestId == null || requestId.isBlank()) {
            // [필수] request_id 누락 insert 금지 → 저장 전에 실패
            throw new IllegalStateException("Missing requestId in MDC");
        }
        return requestId;
    }

    private String buildMetaJson(String comment, String before, String after) {
        try {
            Map<String, Object> meta = new HashMap<>();

            meta.put("comment", comment);

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

}
