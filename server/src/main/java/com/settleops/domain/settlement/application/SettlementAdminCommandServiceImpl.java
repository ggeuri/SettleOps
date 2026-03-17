package com.settleops.domain.settlement.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.settleops.domain.payment.infra.PaymentEventRepository;
import com.settleops.domain.settlement.dto.SettlementBatchRunResponse;
import com.settleops.domain.settlement.dto.SettlementPayActionResponse;
import com.settleops.domain.settlement.entity.Settlement;
import com.settleops.domain.settlement.entity.SettlementBatch;
import com.settleops.domain.settlement.entity.SettlementLine;
import com.settleops.domain.settlement.enums.SettlementBatchResult;
import com.settleops.domain.settlement.enums.SettlementLineType;
import com.settleops.domain.settlement.infra.SettlementBatchRepository;
import com.settleops.domain.settlement.infra.SettlementLineRepository;
import com.settleops.domain.settlement.infra.SettlementRepository;
import com.settleops.global.audit.ActorType;
import com.settleops.global.audit.AuditLogCommand;
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.audit.EntityType;
import com.settleops.global.enums.Action;
import com.settleops.global.enums.ReasonCode;
import com.settleops.global.error.AuditableConflictException;
import com.settleops.global.error.BadRequestException;
import com.settleops.global.error.ConflictException;
import com.settleops.global.error.NotFoundException;
import com.settleops.global.error.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettlementAdminCommandServiceImpl implements SettlementAdminCommandService {

    private static final int TRIGGERED_BY_MAX_LEN = 50;
    private static final String FAIL_REASON_NET_MISMATCH = "NET_MISMATCH";
    private static final String FAIL_REASON_UNEXPECTED_ERROR = "UNEXPECTED_ERROR";

    private final SettlementRepository settlementRepository;
    private final SettlementBatchRepository settlementBatchRepository;
    private final SettlementLineRepository settlementLineRepository;
    private final PaymentEventRepository paymentEventRepository;

    private final AuditLogger auditLogger;
    private final RefundAdjustmentPolicy refundAdjustmentPolicy;
    private final SettlementBatchRunRecorder settlementBatchRunRecorder;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public SettlementBatchRunResponse runBatch(LocalDate baseDate, String requestId) {
        // Trace/Audit SoT: requestId/actorId 스냅샷을 시작 시점에 고정해서 끝까지 동일하게 사용
        validateRequestId(requestId);
        String actorId = currentActorId();

        if (baseDate == null) {
            throw new BadRequestException("baseDate must not be null");
        }

        // triggeredBy (DB 길이 50 가드)
        String triggeredBy = "ADMIN:" + actorId;
        if (triggeredBy.length() > TRIGGERED_BY_MAX_LEN) {
            triggeredBy = triggeredBy.substring(0, TRIGGERED_BY_MAX_LEN);
        }

        // 실행 식별자(runId): 배치 1회 실행을 대표하는 trace 키
        String runId = UUID.randomUUID().toString();

        // 1) 배치 시작 row 생성 (멱등은 여기서 catch → SKIP)
        final SettlementBatch batch;
        try {
            batch = settlementBatchRunRecorder.startOrThrow(baseDate, runId, requestId, triggeredBy);
        } catch (DataIntegrityViolationException e) {
            // 멱등 재실행 정책(LOCKED): 동일 baseDate 재실행은 SKIP (audit_log로만 남김)
            SettlementBatch existing = settlementBatchRepository.findByBatchKey(baseDate)
                    .orElseThrow(() -> new IllegalStateException("batch exists but not found: baseDate=" + baseDate));

            auditBatchAction(
                    requestId,
                    actorId,
                    Action.BATCH_RUN_SKIPPED,
                    baseDate,
                    existing.getRunId(),
                    Map.of("note", "batch_key already exists")
            );

            return new SettlementBatchRunResponse(
                    requestId,
                    existing.getBatchKey(),
                    existing.getRunId(),
                    SettlementBatchRunResponse.RunResult.SKIP,
                    null,
                    existing.getCreatedAt(),
                    existing.getFinishedAt()
            );
        }

        // 2) (선택/권장) 배치 시작 audit
        auditBatchAction(
                requestId,
                actorId,
                Action.BATCH_RUN_TRIGGERED,
                baseDate,
                runId,
                null
        );

        try {
            // 3) CONFIRMED 대상 선정 규칙(LOCKED): payment.status가 아니라 payment_event(PAYMENT_CONFIRMED)
            LocalDateTime from = baseDate.atStartOfDay();
            LocalDateTime to = baseDate.plusDays(1).atStartOfDay();

            List<PaymentEventRepository.ConfirmedPaymentRow> rows =
                    paymentEventRepository.findConfirmedPaymentsByOccurredAtRange(from, to);

            // 4) merchant별 집계 (gross=ΣcapturedAmount)
            Map<String, Long> grossByMerchant = rows.stream()
                    .collect(Collectors.groupingBy(
                            PaymentEventRepository.ConfirmedPaymentRow::getMerchantId,
                            Collectors.summingLong(PaymentEventRepository.ConfirmedPaymentRow::getCapturedAmount)
                    ));

            // 5) settlement 생성 (SoT)
            // settlementNo 결정적 규칙(LOCKED): SET-{yyyyMMdd}-{merchantId}
            String yyyyMMdd = baseDate.toString().replace("-", "");

            List<Settlement> settlements = grossByMerchant.entrySet().stream()
                    .map(e -> {
                        String merchantId = e.getKey();
                        long gross = e.getValue();

                        long fee = 0L;
                        long vat = 0L;
                        long net = gross - fee - vat;

                        String settlementNo = "SET-" + yyyyMMdd + "-" + merchantId;

                        return Settlement.createReady(
                                UUID.randomUUID().toString(),
                                settlementNo,
                                batch.getBatchId(),
                                merchantId,
                                baseDate,
                                gross,
                                fee,
                                vat,
                                net
                        );
                    })
                    .toList();

            settlementRepository.saveAll(settlements);

            // 6) settlement_line 생성
            Map<String, String> settlementIdByMerchant = settlements.stream()
                    .collect(Collectors.toMap(Settlement::getMerchantId, Settlement::getSettlementId));

            List<SettlementLine> paymentLines = rows.stream()
                    .map(r -> SettlementLine.of(
                            settlementIdByMerchant.get(r.getMerchantId()),
                            r.getPaymentId(),
                            SettlementLineType.PAYMENT,
                            r.getCapturedAmount()
                    ))
                    .toList();

            settlementLineRepository.saveAll(paymentLines);

            // 7) 정합성 검증(LOCKED): settlement.net == Σ(line.amount × sign)
            Map<String, Long> sumSignedBySettlementId = paymentLines.stream()
                    .collect(Collectors.groupingBy(
                            SettlementLine::getSettlementId,
                            Collectors.summingLong(SettlementLine::signedAmount)
                    ));

            boolean hasNetMismatch = settlements.stream()
                    .anyMatch(s -> {
                        long sum = sumSignedBySettlementId.getOrDefault(s.getSettlementId(), 0L);
                        return s.getNet() != sum;
                    });

            // 8) 완료 처리(OK/FAIL only)
            if (!hasNetMismatch) {
                settlementBatchRunRecorder.completeOk(runId);

                Map<String, Object> completedMeta = new LinkedHashMap<>(
                        buildBatchCompletedMeta(SettlementBatchResult.OK.name(), null, null)
                );
                completedMeta.put("settlementCount", settlements.size());
                completedMeta.put("paymentLineCount", paymentLines.size());

                auditBatchAction(
                        requestId,
                        actorId,
                        Action.BATCH_RUN_COMPLETED,
                        baseDate,
                        runId,
                        completedMeta
                );

                SettlementBatch finished = settlementBatchRepository.findByRunId(runId)
                        .orElseThrow(() -> new IllegalStateException("batch not found by runId=" + runId));

                return new SettlementBatchRunResponse(
                        requestId,
                        finished.getBatchKey(),
                        finished.getRunId(),
                        SettlementBatchRunResponse.RunResult.OK,
                        null,
                        finished.getCreatedAt(),
                        finished.getFinishedAt()
                );
            }

            String failReason = FAIL_REASON_NET_MISMATCH;
            settlementBatchRunRecorder.completeFail(runId, failReason);

            auditBatchAction(
                    requestId,
                    actorId,
                    Action.BATCH_RUN_COMPLETED,
                    baseDate,
                    runId,
                    buildBatchCompletedMeta(SettlementBatchResult.FAIL.name(), failReason, null)
            );

            SettlementBatch failed = settlementBatchRepository.findByRunId(runId)
                    .orElseThrow(() -> new IllegalStateException("batch not found by runId=" + runId));

            return new SettlementBatchRunResponse(
                    requestId,
                    failed.getBatchKey(),
                    failed.getRunId(),
                    SettlementBatchRunResponse.RunResult.FAIL,
                    failed.getFailReason(),
                    failed.getCreatedAt(),
                    failed.getFinishedAt()
            );

        } catch (Exception e) {
            // 예외도 FAIL로 수렴
            String failReason = FAIL_REASON_UNEXPECTED_ERROR;

            settlementBatchRunRecorder.completeFail(runId, failReason);

            auditBatchAction(
                    requestId,
                    actorId,
                    Action.BATCH_RUN_COMPLETED,
                    baseDate,
                    runId,
                    buildBatchCompletedMeta(
                            SettlementBatchResult.FAIL.name(),
                            failReason,
                            e.getClass().getSimpleName()
                    )
            );

            SettlementBatch failed = settlementBatchRepository.findByRunId(runId)
                    .orElseThrow(() -> new IllegalStateException("batch not found by runId=" + runId));

            return new SettlementBatchRunResponse(
                    requestId,
                    failed.getBatchKey(),
                    failed.getRunId(),
                    SettlementBatchRunResponse.RunResult.FAIL,
                    failed.getFailReason(),
                    failed.getCreatedAt(),
                    failed.getFinishedAt()
            );
        }
    }

    @Override
    @Transactional
    public SettlementPayActionResponse requestPaid(String settlementId, String comment, String requestId) {
        validateRequestId(requestId);
        String actorId = currentActorId();

        if (settlementId == null || settlementId.isBlank()) {
            throw new BadRequestException("settlementId must not be null/blank");
        }

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new NotFoundException("settlement not found"));

        // 1) no-op 200: 이미 PAY_REQUESTED면 현재 상태 반환 (+ audit)
        if (settlement.isPayRequested()) {
            String before = settlement.getStatus().name();
            String after = settlement.getStatus().name();

            auditSettlementAction(
                    requestId, actorId, Action.SETTLEMENT_PAY_REQUESTED,
                    settlement, before, after, comment,
                    true, "ALREADY_PAY_REQUESTED"
            );
            return toPayActionResponse(settlement, requestId);
        }

        // 2) 409 reason 우선순위(LOCKED): HOLD_ACTIVE → NOT_READY → BATCH_FAILED → REFUND_ADJUSTMENT_PENDING
        if (settlement.isHoldActive()) {
            throw new ConflictException(ReasonCode.HOLD_ACTIVE, "hold is active");
        }
        if (!settlement.isReady()) {
            throw new ConflictException(ReasonCode.SETTLEMENT_NOT_READY, "settlement is not READY");
        }
        if (isBatchFailed(settlement.getBatchId())) {
            throw new ConflictException(ReasonCode.BATCH_FAILED, "batch failed");
        }
        if (refundAdjustmentPolicy.isRefundAdjustmentPending(settlementId)) {
            throw new ConflictException(ReasonCode.REFUND_ADJUSTMENT_PENDING, "refund adjustment pending");
        }

        // 3) 상태 전이
        String before = settlement.getStatus().name();
        settlement.requestPaid(actorId, LocalDateTime.now());
        String after = settlement.getStatus().name();

        auditSettlementAction(
                requestId, actorId, Action.SETTLEMENT_PAY_REQUESTED,
                settlement, before, after, comment,
                false, null
        );

        return toPayActionResponse(settlement, requestId);
    }

    @Override
    @Transactional
    public SettlementPayActionResponse approvePaid(String settlementId, String comment, String requestId) {
        validateRequestId(requestId);
        String approverId = currentActorId(); // 한번만 스냅샷(끝까지 동일)

        if (settlementId == null || settlementId.isBlank()) {
            throw new BadRequestException("settlementId must not be null/blank");
        }

        Settlement current = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new NotFoundException("settlement not found"));

        // 1) no-op 200: 이미 PAID (+ audit)
        if (current.isPaid()) {
            String before = current.getStatus().name();
            String after = current.getStatus().name();

            auditSettlementAction(
                    requestId, approverId, Action.SETTLEMENT_PAY_APPROVED,
                    current, before, after, comment,
                    true, "ALREADY_PAID"
            );
            return toPayActionResponse(current, requestId);
        }

        // 2) PAY_REQUESTED 아니면 409
        if (!current.isPayRequested()) {
            throw new ConflictException(ReasonCode.PAY_REQUESTED_REQUIRED, "PAY_REQUESTED status required");
        }

        // 3) PAY_REQUESTED일 때만 락 조회(PESSIMISTIC_WRITE)
        Settlement settlement = settlementRepository.findByIdForUpdate(settlementId)
                .orElseThrow(() -> new NotFoundException("settlement not found"));

        // 락 후 재확인(no-op) (+ audit)
        if (settlement.isPaid()) {
            String before = settlement.getStatus().name();
            String after = settlement.getStatus().name();

            auditSettlementAction(
                    requestId, approverId, Action.SETTLEMENT_PAY_APPROVED,
                    settlement, before, after, comment,
                    true, "ALREADY_PAID"
            );
            return toPayActionResponse(settlement, requestId);
        }

        // 4-eyes 검사
        if (settlement.violatesFourEyes(approverId)) {
            throw new AuditableConflictException(
                    ReasonCode.SAME_APPROVER_NOT_ALLOWED,
                    "requester and approver must be different",
                    ActorType.ADMIN,
                    approverId,
                    Action.SETTLEMENT_PAY_APPROVED,
                    EntityType.SETTLEMENT,
                    settlement.getSettlementId(),
                    settlement.getMerchantId(),
                    settlement.getStatus().name(),
                    settlement.getStatus().name(),
                    comment
            );
        }

        String before = settlement.getStatus().name();
        settlement.approvePaid(approverId, LocalDateTime.now());
        String after = settlement.getStatus().name();

        auditSettlementAction(
                requestId, approverId, Action.SETTLEMENT_PAY_APPROVED,
                settlement, before, after, comment,
                false, null
        );

        return toPayActionResponse(settlement, requestId);
    }

    private boolean isBatchFailed(Long batchId) {
        if (batchId == null) return false;

        return settlementBatchRepository.findById(batchId)
                .filter(b -> b.getFinishedAt() != null) // 완료된 배치만 판정
                .map(b -> b.getResult() == SettlementBatchResult.FAIL)
                .orElse(false);
    }

    private SettlementPayActionResponse toPayActionResponse(Settlement s, String requestId) {
        if (s.isPayRequested() && s.getPaidRequestedAt() == null) {
            throw new IllegalStateException("paidRequestedAt must not be null when status is PAY_REQUESTED");
        }
        if (s.isPaid() && s.getPaidApprovedAt() == null) {
            throw new IllegalStateException("paidAt must not be null when status is PAID");
        }

        LocalDateTime paidAt = s.isPaid() ? s.getPaidApprovedAt() : null;

        return new SettlementPayActionResponse(
                requestId,
                s.getSettlementId(),
                s.getStatus(),
                s.getPaidRequestedAt(),
                paidAt
        );
    }

    /**
     * Settlement 도메인 공통 예외 계층 정렬:
     * - 인증 주체 없음/공백은 UnauthorizedException으로 통일한다.
     * - 통합 스모크 및 GlobalExceptionHandler 계약과 동일한 의미를 유지한다.
     */
    private String currentActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new UnauthorizedException("unauthorized");
        }
        return auth.getName();
    }

    private void auditSettlementAction(
            String requestId,
            String actorId,
            Action action,
            Settlement settlement,
            String before,
            String after,
            String comment,
            boolean noOp,
            String noOpReason
    ) {
        AuditLogCommand cmd = AuditLogCommand.builder()
                .requestId(requestId)
                .merchantId(settlement.getMerchantId())
                .entityType(EntityType.SETTLEMENT)
                .entityId(settlement.getSettlementId())
                .occurredAt(LocalDateTime.now())
                .actorType(ActorType.ADMIN)
                .actorId(actorId)
                .action(action)
                .statusBefore(before)
                .statusAfter(after)
                .metaJson(buildMetaJson(comment, noOp, noOpReason))
                .build();

        auditLogger.log(cmd);
    }

    private void auditBatchAction(
            String requestId,
            String actorId,
            Action action,
            LocalDate baseDate,
            String runId,
            Map<String, Object> extraMeta
    ) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("baseDate", baseDate.toString());
        meta.put("runId", runId);

        if (extraMeta != null && !extraMeta.isEmpty()) {
            meta.putAll(extraMeta);
        }

        // --- 노션(운영 재현 표준) 정합: no-op 표준 키 강제 ---
        // SKIP은 "동일 baseDate 재실행 정책"으로 인해 의도적으로 수행되지 않은(no-op) 케이스
        if (action == Action.BATCH_RUN_SKIPPED) {
            meta.put("noOp", true);
            meta.put("noOpReason", "BATCH_KEY_EXISTS");
        } else {
            meta.put("noOp", false);
        }

        String metaJson;
        try {
            metaJson = objectMapper.writeValueAsString(meta);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize batch audit metaJson", e);
        }

        AuditLogCommand cmd = AuditLogCommand.builder()
                .requestId(requestId)
                .merchantId(null)               // 배치는 baseDate 단위 행위
                .entityType(EntityType.BATCH)
                .entityId(runId)                // 추적 anchor = runId
                .occurredAt(LocalDateTime.now())
                .actorType(ActorType.ADMIN)
                .actorId(actorId)
                .action(action)
                .statusBefore(null)
                .statusAfter(null)
                .metaJson(metaJson)
                .build();

        auditLogger.log(cmd);
    }

    private String buildMetaJson(String comment, boolean noOp, String noOpReason) {
        try {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("comment", (comment == null || comment.isBlank()) ? null : comment);
            meta.put("noOp", noOp);

            if (noOp) {
                meta.put("noOpReason", noOpReason);
            }

            return objectMapper.writeValueAsString(meta);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize audit metaJson", e);
        }
    }

    private void validateRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw new BadRequestException("requestId must not be null/blank");
        }
    }

    private Map<String, Object> buildBatchCompletedMeta(
            String result,
            String failReason,
            String errorType
    ) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("result", result);

        if (failReason != null && !failReason.isBlank()) {
            meta.put("failReason", failReason);
        }

        if (errorType != null && !errorType.isBlank()) {
            meta.put("errorType", errorType);
        }

        return meta;
    }
}