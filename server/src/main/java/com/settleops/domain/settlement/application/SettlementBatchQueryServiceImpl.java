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
    private static final long SKIP_OCCURRED_AT_BUFFER_DAYS = 30L;

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
     * - OK/FAIL 은 settlement_batch.batch_key(baseDate) 기준으로 조회한다.
     * - SKIP 은 audit_log(BATCH_RUN_SKIPPED)에서 조회하되,
     *   최종 필터는 meta_json.baseDate 기준으로 적용한다.
     * - SKIP meta_json 파싱 실패 또는 필수 필드 누락 row는 warn 로그를 남기고
     *   조회 결과에서 제외한다. (기본값 보정 없음)
     * - audit_log.occurredAt 기간 조건은 조회량 제한용 1차 조건일 뿐,
     *   A2 history SoT 는 baseDate 이다.
     * - SKIP 후보 row는 audit_log.occurredAt 기준으로 버퍼 조회한다.
     *   (운영 재실행으로 과거 baseDate의 SKIP이 미래 시점에 기록될 수 있기 때문)
     * - 최종 포함 여부는 meta_json.baseDate 기준으로 판정한다.
     * ...
     */
    @Override
    public SettlementBatchHistoryResponse getHistory(LocalDate from, LocalDate to, Pageable pageable) {
        Range r = normalizeRange(from, to);

        int pageSize = Math.max(1, pageable.getPageSize());
        int pageNumber = Math.max(0, pageable.getPageNumber());
        int fetchSize = (pageNumber + 1) * pageSize; // 전역 페이징을 위해 앞부분을 넉넉히 가져옴

        Pageable fetchPageable = PageRequest.of(0, fetchSize, Sort.unsorted());

        // (1) OK/FAIL: settlement_batch.batch_key(baseDate) 기준
        Page<SettlementBatch> okFailPage = fetchOkFailBatches(r, fetchPageable);

        // (2) SKIP: audit_log 조회 + meta_json.baseDate 최종 필터
        Page<AuditLog> skipPage = fetchSkipLogs(r, fetchPageable);

        // (3) merge
        List<SettlementBatchHistoryRowResponse> merged = new ArrayList<>(okFailPage.getNumberOfElements() + skipPage.getNumberOfElements());

        for (SettlementBatch b : okFailPage.getContent()) {
            SettlementBatchSummaryResponse dto = toBatchSummary(b);
            LocalDateTime occurredAt = (b.getFinishedAt() != null) ? b.getFinishedAt() : b.getCreatedAt();
            merged.add(SettlementBatchHistoryRowResponse.okFail(occurredAt, dto));
        }
        for (AuditLog logRow : skipPage.getContent()) {
            SettlementBatchSkipResponse dto = toSkipResponseOrNull(logRow);
            if (dto == null){
                continue;
            }
            if (!isWithinBaseDateRange(dto.baseDate(), r)) {
                continue;
            }
            merged.add(SettlementBatchHistoryRowResponse.skip(dto.occurredAt(), dto));
        }

        merged.sort(Comparator
                .comparing(SettlementBatchHistoryRowResponse::occurredAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed()
        );

        int start = Math.min(pageNumber * pageSize, merged.size());
        int end = Math.min(start + pageSize, merged.size());
        List<SettlementBatchHistoryRowResponse> content = merged.subList(start, end);

        // SKIP는 occurredAt 버퍼 조회 후 meta_json.baseDate로 재필터링하므로
        // totalElements는 실제 화면 노출 건수와 다를 수 있다. (운영 화면용 근사치)
        long total = okFailPage.getTotalElements() + skipPage.getTotalElements();
        Page<SettlementBatchHistoryRowResponse> page = new PageImpl<>(
                content,
                PageRequest.of(pageNumber, pageSize, Sort.unsorted()),
                total
        );

        return new SettlementBatchHistoryResponse(r.from(), r.toInclusive(), page);
    }

    private Page<SettlementBatch> fetchOkFailBatches(Range r, Pageable pageable) {
        return settlementBatchRepository.findByBatchKeyBetweenOrderByBatchKeyDesc(
                r.from(),
                r.toInclusive(),
                pageable
        );
    }

    private Page<AuditLog> fetchSkipLogs(Range r, Pageable pageable) {
        LocalDateTime fromDt = r.from()
                .minusDays(SKIP_OCCURRED_AT_BUFFER_DAYS)
                .atStartOfDay();

        LocalDateTime toExclusive = r.toInclusive()
                .plusDays(SKIP_OCCURRED_AT_BUFFER_DAYS + 1)
                .atStartOfDay();

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

    /**
     * A2 SKIP row 변환 정책(LOCKED):
     * - audit_log.meta_json 에서 baseDate/runId 를 파싱한다.
     * - meta_json 파싱 실패 또는 필수 필드(baseDate/runId) 누락 시 warn 로그를 남기고
     *   해당 row는 조회 결과에서 제외한다.
     * - 기본값 보정/대체값 주입은 하지 않는다.
     */
    private SettlementBatchSkipResponse toSkipResponseOrNull(AuditLog auditLog) {
        // meta_json: {"baseDate":"YYYY-MM-DD","runId":"...","note":"..."}
        try {
            JsonNode node = objectMapper.readTree(auditLog.getMetaJson());

            String baseDateStr = textOrNull(node, "baseDate");
            String runId = textOrNull(node, "runId");

            if (baseDateStr == null || runId == null) {
                // SKIP 로그는 반드시 baseDate/runId를 가진다는 운영 계약(깨지면 즉시 발견)
                log.warn("skip audit row ignored: baseDate/runId missing. auditId={}, requestId={}",
                        auditLog.getAuditId(), auditLog.getRequestId());
                return null;
            }

            return new SettlementBatchSkipResponse(
                    LocalDate.parse(baseDateStr),
                    runId,
                    auditLog.getRequestId(),
                    auditLog.getActorId(),
                    auditLog.getOccurredAt()
            );
        } catch (Exception e) {
            log.warn("skip audit row ignored: meta_json parse failed. auditId={}, requestId={}",
                    auditLog.getAuditId(), auditLog.getRequestId(), e);
            return null;
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

        // to는 inclusive로 유지한다.
        // occurredAt 기반 버퍼 조회의 exclusive upper bound는 fetchSkipLogs()에서 계산한다.
        return new Range(f, t);
    }

    private record Range(LocalDate from, LocalDate toInclusive) {}

    private boolean isWithinBaseDateRange(LocalDate baseDate, Range r){
        return baseDate != null
                && !baseDate.isBefore(r.from())
                && !baseDate.isAfter(r.toInclusive());
    }
}