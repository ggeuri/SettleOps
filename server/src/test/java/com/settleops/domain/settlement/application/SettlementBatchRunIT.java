package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.dto.SettlementBatchRunResponse;
import com.settleops.domain.settlement.infra.SettlementBatchRepository;
import com.settleops.global.audit.EntityType;
import com.settleops.global.enums.Action;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class SettlementBatchRunIT {

    @Autowired
    SettlementAdminCommandService settlementAdminCommandService;
    @Autowired
    SettlementBatchRepository settlementBatchRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void runBatch_sameBaseDate_twice_should_skip_second_time() {
        // 재현성 고정(자정/타임존 흔들림 제거)
        LocalDate baseDate = LocalDate.of(2026, 3, 2);
        String actorId = "adminA";

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actorId, "N/A")
        );
        SettlementBatchRunResponse r1 = settlementAdminCommandService.runBatch(baseDate, "req-test-001");
        assertThat(r1.runId()).isNotBlank();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actorId, "N/A")
        );
        SettlementBatchRunResponse r2 = settlementAdminCommandService.runBatch(baseDate, "req-test-002");
        assertThat(r2.runId()).isNotBlank();

        String metaJson = jdbcTemplate.queryForObject("""
                        SELECT meta_json
                        FROM audit_log
                        WHERE action = ?
                          AND entity_type = ?
                          AND actor_id = ?
                        ORDER BY occurred_at DESC
                        LIMIT 1
                        """,
                String.class,
                Action.BATCH_RUN_SKIPPED.name(),
                EntityType.BATCH.name(),
                actorId
        );

        assertThat(metaJson).isNotBlank();
        assertThat(metaJson).contains("\"noOp\":true");
        assertThat(metaJson).contains("\"noOpReason\":\"BATCH_KEY_EXISTS\"");
        assertThat(metaJson).contains("\"baseDate\":\"" + baseDate + "\"");

        // 첫 실행은 처리 결과(OK/FAIL), 두 번째는 무조건 SKIP
        assertThat(r1.result()).isIn(
                SettlementBatchRunResponse.RunResult.OK,
                SettlementBatchRunResponse.RunResult.FAIL
        );
        assertThat(r2.result()).isEqualTo(SettlementBatchRunResponse.RunResult.SKIP);

        assertThat(settlementBatchRepository.findByBatchKey(baseDate)).isPresent();
    }
}