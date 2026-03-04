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
import com.settleops.global.error.BadRequestException;
import com.settleops.global.error.ConflictException;
import com.settleops.global.logging.RequestIdKeys;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettlementAdminCommandServiceImpl implements SettlementAdminCommandService {

    private final SettlementRepository settlementRepository;
    private final SettlementBatchRepository settlementBatchRepository;
    private final AuditLogger auditLogger;
    private final RefundAdjustmentPolicy refundAdjustmentPolicy;
    private final SettlementBatchRunRecorder settlementBatchRunRecorder;
    private final ObjectMapper objectMapper;
    private final PaymentEventRepository paymentEventRepository;
    private final SettlementLineRepository settlementLineRepository;

    @Override
    @Transactional
    public SettlementBatchRunResponse runBatch(LocalDate baseDate) {
        // Trace/Audit SoT: requestId(actor/action) 스냅샷을 시작 시점에 고정해서 끝까지 동일하게 사용
        String requestId = currentRequestId();
        String actorId = currentActorId();
        String triggeredBy = "ADMIN:" + actorId;

        // 입력 검증: 형식/필수값 문제는 400(BAD_REQUEST)로 처리(상태/가드레일 위반과 혼용 금지)
        if (baseDate == null) {
            throw new BadRequestException("baseDate must not be null");
        }

        // 실행 식별자(runId): 배치 1회 실행을 대표하는 trace 키(응답/감사에서 anchor로 사용)
        String runId = UUID.randomUUID().toString();

        /*
         * 배치 시작 레코드 생성(멱등 + 동시성 레이스 처리 포함)
         * - batch_key(baseDate) UNIQUE 기반
         * - 이미 존재하면 created=false로 반환(호출자는 SKIP 처리)
         * - created=true면 saveAndFlush로 batchId까지 확정된 row를 반환
         */
        SettlementBatchRunRecorder.StartResult start =
                settlementBatchRunRecorder.start(baseDate, runId, requestId, triggeredBy);

        /*
         * 멱등 재실행 정책(LOCKED):
         * - 동일 baseDate 재실행은 SKIP
         * - SKIP 이력은 settlement_batch가 아니라 audit_log(Action=BATCH_RUN_SKIPPED)로만 남김
         */
        if (!start.created()) {
            SettlementBatch existing = start.batch();

            auditBatchAction(
                    requestId,
                    actorId,
                    Action.BATCH_RUN_SKIPPED,
                    baseDate,
                    existing.getRunId(),
                    "batch_key already exists"
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

        // batchId가 확정된 상태(REQUIRES_NEW + saveAndFlush) → settlement 생성 시 batchId를 안전하게 참조 가능
        SettlementBatch batch = start.batch();

        // 운영 재현(선택/권장): 배치 시작 시점의 행위를 audit_log에 남겨 requestId 단위 타임라인을 완성
        auditBatchAction(
                requestId,
                actorId,
                Action.BATCH_RUN_TRIGGERED,
                baseDate,
                runId,
                null
        );

        try {
            /*
             * A2 핵심: CONFIRMED 대상 선정 규칙(LOCKED)
             * - payment.status가 아니라 payment_event(PAYMENT_CONFIRMED)로만 판정
             * - baseDate(KST) 경계: [00:00, next 00:00)로 고정
             */
            LocalDateTime from = baseDate.atStartOfDay();
            LocalDateTime to = baseDate.plusDays(1).atStartOfDay();

            List<PaymentEventRepository.ConfirmedPaymentRow> rows =
                    paymentEventRepository.findConfirmedPaymentsByOccurredAtRange(from, to);

            // merchant별 집계(gross=ΣcapturedAmount). 금액은 KRW 정수(long) 고정
            Map<String, Long> grossByMerchant = rows.stream()
                    .collect(Collectors.groupingBy(
                            PaymentEventRepository.ConfirmedPaymentRow::getMerchantId,
                            Collectors.summingLong(PaymentEventRepository.ConfirmedPaymentRow::getCapturedAmount)
                    ));

            /*
             * settlement 생성(SoT): 배치 시점에 계산된 집계 결과를 settlement 헤더(gross/fee/vat/net)에 저장
             * - fee/vat 룰이 확정되기 전 MVP에서는 0으로 두고, 추후 정책 확정 시 계산식을 반영
             */
            List<Settlement> settlements = grossByMerchant.entrySet().stream()
                    .map(e -> {
                        String merchantId = e.getKey();
                        long gross = e.getValue();

                        long fee = 0L;
                        long vat = 0L;
                        long net = gross - fee - vat;

                        return Settlement.createReady(
                                UUID.randomUUID().toString(),
                                "SET-" + baseDate + "-" + merchantId + "-" + UUID.randomUUID().toString().substring(0, 8),
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

            // settlement_line 생성 준비: merchantId → settlementId 매핑
            Map<String, String> settlementIdByMerchant = settlements.stream()
                    .collect(Collectors.toMap(
                            Settlement::getMerchantId,
                            Settlement::getSettlementId
                    ));

            /*
             * settlement_line(PAYMENT) 적재
             * - amount는 항상 양수 저장, 부호는 lineType(PAYMENT:+ / REFUND:-)로만 해석
             */
            List<SettlementLine> paymentLines = rows.stream()
                    .map(r -> SettlementLine.of(
                            settlementIdByMerchant.get(r.getMerchantId()),
                            r.getPaymentId(),
                            SettlementLineType.PAYMENT,
                            r.getCapturedAmount()
                    ))
                    .toList();

            settlementLineRepository.saveAll(paymentLines);

            /*
             * 정합성 검증(LOCKED): settlement.net == Σ(line.amount × sign)
             * - gross/fee/vat는 line 합으로 검증하지 않음(MVP 금지)
             */
            Map<String, Long> sumSignedBySettlementId = paymentLines.stream()
                    .collect(Collectors.groupingBy(
                            SettlementLine::getSettlementId,
                            Collectors.summingLong(SettlementLine::signedAmount)
                    ));

            List<String> mismatches = settlements.stream()
                    .filter(s -> {
                        long sum = sumSignedBySettlementId.getOrDefault(s.getSettlementId(), 0L);
                        return s.getNet() != sum;
                    })
                    .map(s -> {
                        long sum = sumSignedBySettlementId.getOrDefault(s.getSettlementId(), 0L);
                        return "SETTLEMENT_MISMATCH settlementId=%s net=%d sumLines=%d"
                                .formatted(s.getSettlementId(), s.getNet(), sum);
                    })
                    .toList();

            if (mismatches.isEmpty()) {
                // 배치 완료 기록은 Recorder가 전담(REQUIRES_NEW). 본 트랜잭션 롤백과 분리해 운영 재현성을 보호
                settlementBatchRunRecorder.completeOk(runId);

                auditBatchAction(
                        requestId,
                        actorId,
                        Action.BATCH_RUN_COMPLETED,
                        baseDate,
                        runId,
                        "OK: settlements=" + settlements.size() + ", paymentLines=" + paymentLines.size()
                );

                // 응답은 완료 상태(finishedAt 포함)를 보장하기 위해 최신 배치 row를 재조회
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

            // FAIL: mismatch 내용을 failReason으로 저장(운영 점검/재현용)
            String failReason = String.join(" | ", mismatches);

            settlementBatchRunRecorder.completeFail(runId, failReason);

            auditBatchAction(requestId, actorId, Action.BATCH_RUN_COMPLETED, baseDate, runId, "FAIL");

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
            // 예외 케이스도 배치 결과(FAIL)로 수렴시켜 운영 대응을 단순화(OK/FAIL only)
            String failReason = "UNEXPECTED_ERROR: " + e.getClass().getSimpleName();

            settlementBatchRunRecorder.completeFail(runId, failReason);

            auditBatchAction(
                    requestId,
                    actorId,
                    Action.BATCH_RUN_COMPLETED,
                    baseDate,
                    runId,
                    "FAIL: " + failReason
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
    public SettlementPayActionResponse requestPaid(String settlementId, String comment) {
        // LOCKED fail-fast (request_id / actor_id SoT)
        String requestId = currentRequestId();
        String actorId = currentActorId();

        if (settlementId == null || settlementId.isBlank()) {
            throw new BadRequestException("settlementId must not be null/blank");
        }

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "settlement not found"));

        // 1) no-op 200: 이미 PAY_REQUESTED면 현재 상태 반환 (+ audit)
        if (settlement.isPayRequested()) {
            String before = settlement.getStatus().name();
            String after = settlement.getStatus().name();

            auditSettlementAction(
                    requestId,
                    actorId,
                    Action.SETTLEMENT_PAY_REQUESTED,
                    settlement,
                    before,
                    after,
                    comment
            );
            return toPayActionResponse(settlement, requestId);
        }

        // 2) 409 reason 우선순위 고정(LOCKED)
        //    HOLD_ACTIVE → SETTLEMENT_NOT_READY → BATCH_FAILED → REFUND_ADJUSTMENT_PENDING

        if (settlement.isHoldActive()) {
            throw new ConflictException(ReasonCode.HOLD_ACTIVE, "hold is active");
        }

        if (!settlement.isReady()) {
            throw new ConflictException(ReasonCode.SETTLEMENT_NOT_READY, "settlement is not READY");
        }

        if (isBatchFailed(settlement.getBaseDate())) {
            throw new ConflictException(ReasonCode.BATCH_FAILED, "batch failed");
        }

        if (refundAdjustmentPolicy.isRefundAdjustmentPending(settlementId)) {
            throw new ConflictException(ReasonCode.REFUND_ADJUSTMENT_PENDING, "refund adjustment pending");
        }

        // 3) 상태 전이
        String before = settlement.getStatus().name();
        settlement.requestPaid(actorId, LocalDateTime.now());
        String after = settlement.getStatus().name();

        // 4) audit (LOCKED)
        auditSettlementAction(
                requestId,
                actorId,
                Action.SETTLEMENT_PAY_REQUESTED,
                settlement,
                before,
                after,
                comment
        );

        return toPayActionResponse(settlement, requestId);
    }

    @Override
    @Transactional
    public SettlementPayActionResponse approvePaid(String settlementId, String comment) {
        // LOCKED fail-fast (request_id / actor_id SoT)
        String requestId = currentRequestId();
        String approverId = currentActorId(); // 한번만 스냅샷(끝까지 동일하게 사용)

        if (settlementId == null || settlementId.isBlank()) {
            throw new BadRequestException("settlementId must not be null/blank");
        }

        Settlement current = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "settlement not found"));

        // 1) no-op 200: 이미 PAID (+ audit)
        if (current.isPaid()) {
            String before = current.getStatus().name();
            String after = current.getStatus().name();

            auditSettlementAction(
                    requestId,
                    approverId,
                    Action.SETTLEMENT_PAY_APPROVED,
                    current,
                    before,
                    after,
                    comment
            );
            return toPayActionResponse(current, requestId);
        }

        // 2) PAY_REQUESTED 아니면 409
        if (!current.isPayRequested()) {
            throw new ConflictException(ReasonCode.PAY_REQUESTED_REQUIRED, "PAY_REQUESTED status required");
        }

        // 3) PAY_REQUESTED일 때만 락 조회(PESSIMISTIC_WRITE)
        Settlement settlement = settlementRepository.findByIdForUpdate(settlementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "settlement not found"));

        // 락 후 재확인(no-op) (+ audit)
        if (settlement.isPaid()) {
            String before = settlement.getStatus().name();
            String after = settlement.getStatus().name();

            auditSettlementAction(
                    requestId,
                    approverId,
                    Action.SETTLEMENT_PAY_APPROVED,
                    settlement,
                    before,
                    after,
                    comment
            );
            return toPayActionResponse(settlement, requestId);
        }

        // 4-eyes 검사: requester != approver
        if (settlement.violatesFourEyes(approverId)) {
            throw new ConflictException(
                    ReasonCode.SAME_APPROVER_NOT_ALLOWED,
                    "requester and approver must be different"
            );
        }

        String before = settlement.getStatus().name();
        settlement.approvePaid(approverId, LocalDateTime.now());
        String after = settlement.getStatus().name();

        auditSettlementAction(
                requestId,
                approverId,
                Action.SETTLEMENT_PAY_APPROVED,
                settlement,
                before,
                after,
                comment
        );

        return toPayActionResponse(settlement, requestId);
    }

    private boolean isBatchFailed(LocalDate baseDate) {
        if (baseDate == null) return false;
        return settlementBatchRepository.findByBatchKey(baseDate)
                .filter(b -> b.getFinishedAt() != null) // 실행 완료된 배치만 판정(선택)
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

    private String currentRequestId() {
        String requestId = MDC.get(RequestIdKeys.MDC_KEY);
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalStateException("requestId must not be null/blank (provided by RequestIdFilter)");
        }
        return requestId;
    }

    private String currentActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new IllegalStateException("actorId must not be null/blank");
        }
        return auth.getName();
    }

    /**
     * requestId/actorId를 호출자가 스냅샷으로 넘겨주도록 강제
     * - 동시성/스레드 전환/컨텍스트 꼬임이 있어도 audit이 흔들리지 않게 한다.
     */
    private void auditSettlementAction(
            String requestId,
            String actorId,
            Action action,
            Settlement settlement,
            String before,
            String after,
            String comment
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
                .metaJson(buildMetaJson(comment))
                .build();

        auditLogger.log(cmd);
    }

    private void auditBatchAction(
            String requestId,
            String actorId,
            Action action,
            LocalDate baseDate,
            String runId,
            String note
    ) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("baseDate", baseDate.toString());
        meta.put("runId", runId);
        if (note != null && !note.isBlank()) {
            meta.put("note", note);
        }

        String metaJson;
        try {
            metaJson = objectMapper.writeValueAsString(meta);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize batch audit metaJson", e);
        }

        AuditLogCommand cmd = AuditLogCommand.builder()
                .requestId(requestId)
                .merchantId(null) // 배치는 merchant 단위 행위가 아니라 baseDate 단위 행위
                .entityType(EntityType.BATCH)
                .entityId(runId) // 추적은 runId가 제일 명확
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

    private String buildMetaJson(String comment) {
        try {
            Map<String, Object> meta = new HashMap<>();
            if (comment != null && !comment.isBlank()) {
                meta.put("comment", comment);
            }
            return objectMapper.writeValueAsString(meta);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize audit metaJson", e);
        }
    }

    // 현재 runBatch는 TODO지만, 이미 클래스에 존재하므로 유지 (컴파일/리팩토링 안전)
    @SuppressWarnings("unused")
    private SettlementBatchRunResponse toBatchRunResponse(
            String requestId,
            SettlementBatch batch,
            SettlementBatchRunResponse.RunResult result
    ) {
        String failReason = (result == SettlementBatchRunResponse.RunResult.FAIL) ? batch.getFailReason() : null;

        return new SettlementBatchRunResponse(
                requestId,
                batch.getBatchKey(),
                batch.getRunId(),
                result,
                failReason,
                batch.getCreatedAt(),
                batch.getFinishedAt()
        );
    }
}