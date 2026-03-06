package com.settleops.domain.settlement.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.settleops.domain.settlement.dto.SettlementBatchHistoryResponse;
import com.settleops.domain.settlement.dto.SettlementBatchHistoryRowResponse;
import com.settleops.domain.settlement.dto.SettlementBatchSkipResponse;
import com.settleops.domain.settlement.dto.SettlementBatchSummaryResponse;
import com.settleops.domain.settlement.entity.SettlementBatch;
import com.settleops.domain.settlement.infra.SettlementBatchRepository;
import com.settleops.global.audit.AuditLog;
import com.settleops.global.audit.AuditLogQuery;
import com.settleops.global.audit.EntityType;
import com.settleops.global.enums.Action;
import com.settleops.global.error.BadRequestException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
public class SettlementBatchQueryServiceImpl implements SettlementBatchQueryService {

    private final SettlementBatchRepository settlementBatchRepository;
    private final AuditLogQuery auditLogQuery;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SettlementBatchQueryServiceImpl(
            SettlementBatchRepository settlementBatchRepository,
            @Qualifier("auditLogRepositoryImpl") AuditLogQuery auditLogQuery,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.settlementBatchRepository = settlementBatchRepository;
        this.auditLogQuery = auditLogQuery;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }
    /**
     * A2 통합(OK/FAIL + SKIP) - 한 화면
     *
     * 구현 방침(운영 안전):
     * - settlement_batch(OK/FAIL) vs audit_log(SKIP)는 서로 다른 SoT라 DB에서 "진짜 전역 페이징"이 어렵다.
     * - 대신 (pageNumber+1)*pageSize 만큼만 양쪽에서 가져와 merge+sort 후, 마지막에 slice 해서 전역 페이징을 보장한다.
     * - 기간 기본값은 최근 7일(오늘 포함 7일): [today-6, today]
     */
    @Override
    public SettlementBatchHistoryResponse getHistory(LocalDate from, LocalDate to, Pageable pageable) {
        Range r = normalizeRange(from, to);

        int pageSize = Math.max(1, pageable.getPageSize());
        int pageNumber = Math.max(0, pageable.getPageNumber());
        int fetchSize = (pageNumber + 1) * pageSize; // 전역 페이징을 위해 앞부분을 넉넉히 가져옴

        Pageable fetchPageable = PageRequest.of(0, fetchSize, Sort.unsorted());

        // (1) OK/FAIL: settlement_batch 기반 (createdAt 기간)
        Page<SettlementBatch> okFailPage = fetchOkFailBatches(r, fetchPageable);

        // (2) SKIP: audit_log 기반 (occurredAt 기간)
        Page<AuditLog> skipPage = fetchSkipLogs(r, fetchPageable);

        // (3) merge
        List<SettlementBatchHistoryRowResponse> merged = new ArrayList<>(okFailPage.getNumberOfElements() + skipPage.getNumberOfElements());

        for (SettlementBatch b : okFailPage.getContent()) {
            SettlementBatchSummaryResponse dto = toBatchSummary(b);
            LocalDateTime occurredAt = (b.getFinishedAt() != null) ? b.getFinishedAt() : b.getCreatedAt();
            merged.add(SettlementBatchHistoryRowResponse.okFail(occurredAt, dto));
        }
        for (AuditLog logRow : skipPage.getContent()) {
            try {
                SettlementBatchSkipResponse dto = toSkipResponse(logRow);
                merged.add(SettlementBatchHistoryRowResponse.skip(dto.occurredAt(), dto));
            } catch (Exception e) {
                // 운영 화면 안정: SKIP meta_json 파싱 실패는 해당 row만 제외하고 계속 진행
                // (권장) 최소 로그 남겨서 발견 가능하게
                log.warn("skip audit meta_json parse failed. auditId={}, requestId={}",
                        logRow.getAuditId(), logRow.getRequestId(), e);
            }
        }

        merged.sort(Comparator
                .comparing(SettlementBatchHistoryRowResponse::occurredAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed()
        );

        int start = Math.min(pageNumber * pageSize, merged.size());
        int end = Math.min(start + pageSize, merged.size());
        List<SettlementBatchHistoryRowResponse> content = merged.subList(start, end);

        // NOTE: 서로 다른 SoT(OK/FAIL vs SKIP) 병합이라 totalElements는 "근사치"일 수 있음(운영 화면용)
        long total = okFailPage.getTotalElements() + skipPage.getTotalElements();
        Page<SettlementBatchHistoryRowResponse> page = new PageImpl<>(
                content,
                PageRequest.of(pageNumber, pageSize, Sort.unsorted()),
                total
        );

        return new SettlementBatchHistoryResponse(r.from(), r.toInclusive(), page);
    }

    private Page<SettlementBatch> fetchOkFailBatches(Range r, Pageable pageable) {
        LocalDateTime fromDt = r.from().atStartOfDay();
        LocalDateTime toExclusive = r.toExclusive().atStartOfDay();

        return settlementBatchRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(fromDt, toExclusive, pageable);
    }

    private Page<AuditLog> fetchSkipLogs(Range r, Pageable pageable) {
        LocalDateTime fromDt = r.from().atStartOfDay();
        LocalDateTime toExclusive = r.toExclusive().atStartOfDay();

        return auditLogQuery.findByActionAndEntityTypeAndOccurredAtBetweenOrderByOccurredAtDesc(
                Action.BATCH_RUN_SKIPPED,
                EntityType.BATCH,
                fromDt,
                toExclusive,
                pageable
        );
    }

    private SettlementBatchSummaryResponse toBatchSummary(SettlementBatch b) {
        return new SettlementBatchSummaryResponse(
                b.getBatchId(),
                b.getBatchKey(),
                b.getRunId(),
                b.getResult(),
                b.getCreatedAt(),
                b.getFinishedAt(),
                b.getTriggeredBy(),
                b.getRequestId(),
                b.getFailReason()
        );
    }

    private SettlementBatchSkipResponse toSkipResponse(AuditLog log) {
        // meta_json: {"baseDate":"YYYY-MM-DD","runId":"...","note":"..."}
        try {
            JsonNode node = objectMapper.readTree(log.getMetaJson());

            String baseDateStr = textOrNull(node, "baseDate");
            String runId = textOrNull(node, "runId");

            if (baseDateStr == null || runId == null) {
                // SKIP 로그는 반드시 baseDate/runId를 가진다는 운영 계약(깨지면 즉시 발견)
                throw new IllegalStateException("BATCH_RUN_SKIPPED meta_json must contain baseDate/runId");
            }

            return new SettlementBatchSkipResponse(
                    LocalDate.parse(baseDateStr),
                    runId,
                    log.getRequestId(),
                    log.getActorId(),
                    log.getOccurredAt()
            );
        } catch (Exception e) {
            throw new IllegalStateException("failed to parse audit_log.meta_json for BATCH_RUN_SKIPPED", e);
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    /**
     * A2 운영 UX 안전장치(노션 계약):
     * - merchantId 검색처럼 기본 기간 제한이 필요하지만,
     *   A2는 batch 콘솔이라서 from/to를 반드시 받는 방식으로 가는 걸 추천.
     * - 그래도 누락되면 최근 7일 기본값 적용(운영 안전).
     */
    private Range normalizeRange(LocalDate from, LocalDate to) {
        // 서버 timezone을 KST로 고정해두는 게 전제(또는 아래처럼 명시)
        LocalDate today = LocalDate.now(clock);
        LocalDate defaultFrom = today.minusDays(6);
        LocalDate defaultTo = today;

        LocalDate f = (from == null) ? defaultFrom : from;
        LocalDate t = (to == null) ? defaultTo : to;

        if (t.isBefore(f)) {
            throw new BadRequestException("to must be >= from");
        }

        // to는 inclusive로 받되, occurredAt 조회는 [from, to+1) 형태로 변환
        return new Range(f, t, t.plusDays(1));
    }

    private record Range(LocalDate from, LocalDate toInclusive, LocalDate toExclusive) {}
}