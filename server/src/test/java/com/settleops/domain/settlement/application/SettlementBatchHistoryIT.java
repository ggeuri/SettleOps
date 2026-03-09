package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.dto.*;
import com.settleops.domain.settlement.infra.SettlementBatchRepository;
import com.settleops.global.audit.EntityType;
import com.settleops.global.enums.Action;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
public class SettlementBatchHistoryIT {

    @Autowired SettlementAdminCommandService settlementAdminCommandService;
    @Autowired SettlementBatchQueryService settlementBatchQueryService;
    @Autowired SettlementBatchRepository settlementBatchRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired Clock clock;
    @Autowired MockMvc mockMvc;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("A2: 배치 이력 통합조회(OK/FAIL + SKIP) - 같은 baseDate 재실행 시 SKIP가 함께 노출되고 occurredAt desc로 정렬된다")
    void history_should_include_okFail_and_skip_and_sort_by_occurredAt_desc() {
        // given (Clock 기준 오늘로 고정)
        LocalDate baseDate = LocalDate.now(clock);
        String actorId = ("adminA-" + UUID.randomUUID().toString().substring(0, 8));

        // 1회차: OK/FAIL
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actorId, "N/A"));
        SettlementBatchRunResponse first = settlementAdminCommandService.runBatch(baseDate, "req-test-001");

        // 2회차: SKIP
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actorId, "N/A"));
        SettlementBatchRunResponse second = settlementAdminCommandService.runBatch(baseDate, "req-test-002");

        // --- SoT 검증(정석) ---
        // OK/FAIL SoT: settlement_batch 존재
        var persistedBatch = settlementBatchRepository.findByBatchKey(baseDate).orElseThrow();
        assertThat(persistedBatch.getRunId()).isNotBlank();

        // SKIP SoT: audit_log에 BATCH_RUN_SKIPPED가 "해당 날짜 범위"에 존재
        LocalDateTime from = baseDate.atStartOfDay();
        LocalDateTime toExclusive = baseDate.plusDays(1).atStartOfDay();

        String action = Action.BATCH_RUN_SKIPPED.name();
        String entityType = EntityType.BATCH.name();

        Long skipCount = jdbcTemplate.queryForObject("""
        SELECT COUNT(*)
        FROM audit_log
        WHERE action = ?
          AND entity_type = ?
          AND occurred_at >= ?
          AND occurred_at < ?
          AND actor_id = ?
        """, Long.class, action, entityType, Timestamp.valueOf(from), Timestamp.valueOf(toExclusive), actorId);

        assertThat(skipCount).isNotNull();
        assertThat(skipCount).isGreaterThanOrEqualTo(1L);

        // when: history 조회 (표시 품질 검증)
        SettlementBatchHistoryResponse res = settlementBatchQueryService.getHistory(
                baseDate, baseDate, PageRequest.of(0, 50)
        );

        // then: 응답 기본
        assertThat(res).isNotNull();
        assertThat(res.page()).isNotNull();
        assertThat(res.from()).isEqualTo(baseDate);
        assertThat(res.to()).isEqualTo(baseDate);

        List<SettlementBatchHistoryRowResponse> rows = res.page().getContent();
        assertThat(rows).isNotEmpty();

        // 정렬(occurredAt desc)
        for (int i = 0; i < rows.size() - 1; i++) {
            assertThat(rows.get(i).occurredAt()).isAfterOrEqualTo(rows.get(i + 1).occurredAt());
        }

        // (표시 품질) SKIP row가 실제로 매핑되어 나오는지 + runId가 "persisted batch runId"와 일치하는지
        String persistedRunId = persistedBatch.getRunId();

        SettlementBatchHistoryRowResponse skipRow = rows.stream()
                .filter(r -> r.type() == SettlementBatchHistoryRowResponse.RowType.SKIP)
                .findFirst()
                .orElseThrow(() -> new AssertionError("history page must include SKIP row when skip SoT exists"));

        assertThat(skipRow.skip()).isNotNull();
        assertThat(skipRow.skip().baseDate()).isEqualTo(baseDate);
        assertThat(skipRow.skip().runId()).isEqualTo(persistedRunId);

        // sanity: 2번째 결과는 SKIP
        assertThat(second.result()).isEqualTo(SettlementBatchRunResponse.RunResult.SKIP);
        // 1번째는 OK/FAIL
        assertThat(first.result()).isIn(SettlementBatchRunResponse.RunResult.OK, SettlementBatchRunResponse.RunResult.FAIL);
    }

    @Test
    @WithMockUser(username = "adminA", roles = "ADMIN")
    @DisplayName("A2 history 응답에는 no-store 헤더가 적용된다")
    void getHistory_appliesNoStoreHeaders() throws Exception {
        mockMvc.perform(get("/api/admin/settlement-batches/history")
                        .param("from", LocalDate.now(clock).minusDays(6).toString())
                        .param("to", LocalDate.now(clock).toString())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("Expires", "0"));
    }
}