package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.dto.SettlementBatchHistoryResponse;
import com.settleops.domain.settlement.dto.SettlementBatchHistoryRowResponse;
import com.settleops.domain.settlement.dto.SettlementBatchRunResponse;
import com.settleops.domain.settlement.infra.SettlementBatchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
class SettlementBatchHistoryIT {

    @Autowired
    SettlementAdminCommandService settlementAdminCommandService;

    @Autowired
    SettlementBatchQueryService settlementBatchQueryService;

    @Autowired
    SettlementBatchRepository settlementBatchRepository;

    @Autowired
    Clock clock;

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("A2: 배치 이력 통합조회(OK/FAIL + SKIP) - 같은 baseDate 재실행 시 SKIP가 함께 노출되고 occurredAt desc로 정렬된다")
    void history_should_include_okFail_and_skip_and_sort_by_occurredAt_desc() {
        // given
        LocalDate baseDate = nextAvailableBaseDateWithin30Days();
        String actorId = "adminA-" + UUID.randomUUID().toString().substring(0, 8);

        SettlementBatchRunResponse first = settlementAdminCommandService.runBatch(baseDate, "req-test-001", actorId);
        SettlementBatchRunResponse second = settlementAdminCommandService.runBatch(baseDate, "req-test-002", actorId);

        // sanity
        assertThat(first.result()).isIn(
                SettlementBatchRunResponse.RunResult.OK,
                SettlementBatchRunResponse.RunResult.FAIL
        );
        assertThat(second.result()).isEqualTo(SettlementBatchRunResponse.RunResult.SKIP);

        var persistedBatch = settlementBatchRepository.findByBatchKey(baseDate).orElseThrow();
        assertThat(persistedBatch.getRunId()).isNotBlank();

        // when
        SettlementBatchHistoryResponse res = settlementBatchQueryService.getHistory(
                baseDate,
                baseDate,
                PageRequest.of(0, 50)
        );

        // then
        assertThat(res).isNotNull();
        assertThat(res.page()).isNotNull();
        assertThat(res.from()).isEqualTo(baseDate);
        assertThat(res.to()).isEqualTo(baseDate);

        List<SettlementBatchHistoryRowResponse> rows = res.page().getContent();
        assertThat(rows).isNotEmpty();

        for (int i = 0; i < rows.size() - 1; i++) {
            assertThat(rows.get(i).occurredAt()).isAfterOrEqualTo(rows.get(i + 1).occurredAt());
        }

        assertThat(rows.stream()
                .filter(r -> r.type() == SettlementBatchHistoryRowResponse.RowType.OK_FAIL)
                .filter(r -> r.batch() != null)
                .anyMatch(r ->
                        r.batch().batchKey().isEqual(baseDate)
                                && r.batch().runId().equals(persistedBatch.getRunId())
                ))
                .isTrue();

        assertThat(rows.stream()
                .filter(r -> r.type() == SettlementBatchHistoryRowResponse.RowType.SKIP)
                .filter(r -> r.skip() != null)
                .anyMatch(r ->
                        r.skip().baseDate().isEqual(baseDate)
                                && r.skip().runId().equals(persistedBatch.getRunId())
                ))
                .isTrue();
    }

    @Test
    @DisplayName("A2 history 응답에는 no-store 헤더가 적용된다")
    void getHistory_appliesNoStoreHeaders() throws Exception {
        mockMvc.perform(get("/api/admin/settlement-batches/history")
                        .sessionAttr("ROLE", "ADMIN")
                        .sessionAttr("ADMIN_ID", "adminA")
                        .param("from", LocalDate.now(clock).minusDays(6).toString())
                        .param("to", LocalDate.now(clock).toString())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("Expires", "0"));
    }

    private LocalDate nextAvailableBaseDateWithin30Days() {
        LocalDate start = LocalDate.now(clock).plusDays(1);

        for (int i = 0; i < 30; i++) {
            LocalDate candidate = start.plusDays(i);
            if (settlementBatchRepository.findByBatchKey(candidate).isEmpty()) {
                return candidate;
            }
        }

        throw new AssertionError("향후 30일 내 사용 가능한 baseDate를 찾지 못했습니다.");
    }
}