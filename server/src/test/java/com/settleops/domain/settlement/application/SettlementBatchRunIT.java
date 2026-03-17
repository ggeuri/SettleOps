package com.settleops.domain.settlement.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
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
    void runBatch_sameBaseDate_twice_should_skip_second_time() throws Exception {
        LocalDate baseDate = LocalDate.of(2026, 3, 2);
        String actorId = "adminA";
        String firstRequestId = "req-test-001";
        String secondRequestId = "req-test-002";

        assertThat(settlementBatchRepository.findByBatchKey(baseDate)).isEmpty();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actorId, "N/A")
        );
        SettlementBatchRunResponse r1 = settlementAdminCommandService.runBatch(baseDate, firstRequestId);
        assertThat(r1.runId()).isNotBlank();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actorId, "N/A")
        );
        SettlementBatchRunResponse r2 = settlementAdminCommandService.runBatch(baseDate, secondRequestId);
        assertThat(r2.runId()).isNotBlank();

        String metaJson = jdbcTemplate.queryForObject("""
                SELECT meta_json
                FROM audit_log
                WHERE action = ?
                  AND entity_type = ?
                  AND actor_id = ?
                  AND request_id = ?
                ORDER BY occurred_at DESC
                LIMIT 1
                """,
                String.class,
                Action.BATCH_RUN_SKIPPED.name(),
                EntityType.BATCH.name(),
                actorId,
                secondRequestId
        );

        assertThat(metaJson).isNotBlank();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode metaNode = mapper.readTree(metaJson);

        if (metaNode.isTextual()) {
            metaNode = mapper.readTree(metaNode.asText());
        }

        assertThat(metaNode.get("noOp").asBoolean()).isTrue();
        assertThat(metaNode.get("noOpReason").asText()).isEqualTo("BATCH_KEY_EXISTS");
        assertThat(metaNode.get("baseDate").asText()).isEqualTo(baseDate.toString());

        assertThat(r1.result()).isIn(
                SettlementBatchRunResponse.RunResult.OK,
                SettlementBatchRunResponse.RunResult.FAIL
        );
        assertThat(r2.result()).isEqualTo(SettlementBatchRunResponse.RunResult.SKIP);

        assertThat(settlementBatchRepository.findByBatchKey(baseDate)).isPresent();
    }
}