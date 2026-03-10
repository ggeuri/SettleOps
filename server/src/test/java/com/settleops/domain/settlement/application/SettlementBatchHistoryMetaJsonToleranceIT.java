package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.dto.SettlementBatchHistoryResponse;
import com.settleops.domain.settlement.dto.SettlementBatchRunResponse;
import com.settleops.domain.settlement.infra.SettlementBatchRepository;
import com.settleops.global.audit.ActorType;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SettlementBatchHistoryMetaJsonToleranceIT {

    @Autowired SettlementAdminCommandService settlementAdminCommandService;
    @Autowired SettlementBatchQueryService settlementBatchQueryService;
    @Autowired SettlementBatchRepository settlementBatchRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired Clock clock;

    @AfterEach
    void tearDown(){
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("A2: SKIP audit_log meta_json이 깨져도 history 조회는 깨지지 않고(운영 안정) OK/FAIL SoT는 보장된다")
    void history_should_not_fail_when_skip_meta_json_is_invalid() {
        // given
        LocalDate baseDate = LocalDate.now(clock).minusDays(10);
        String actorId = "adminA-" + UUID.randomUUID().toString().substring(0, 8);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actorId, "N/A")
        );

        SettlementBatchRunResponse first = settlementAdminCommandService.runBatch(baseDate, "req-test-001");
        assertThat(first.result()).isIn(SettlementBatchRunResponse.RunResult.OK, SettlementBatchRunResponse.RunResult.FAIL);
        assertThat(first.runId()).isNotBlank();

        // OK/FAIL SoT: settlement_batch는 반드시 존재해야 함
        assertThat(settlementBatchRepository.findByBatchKey(baseDate)).isPresent();

        // audit_log에 "깨진 SKIP 로그" 강제 주입 (meta_json이 JSON이 아님)
        // 컬럼명: audit_id(PK AI), request_id, occurred_at, actor_type, actor_id, action, entity_type, entity_id, meta_json, (status_before/after/merchant_id는 nullable)
        LocalDateTime now = baseDate.atTime(10, 0);
        LocalDateTime from = baseDate.atStartOfDay();
        LocalDateTime toExclusive = baseDate.plusDays(1).atStartOfDay();

        int inserted = jdbcTemplate.update("""
                INSERT INTO audit_log
                (request_id, occurred_at, actor_type, actor_id, action, entity_type, entity_id, meta_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID().toString(),
                Timestamp.valueOf(now),
                ActorType.ADMIN.name(),
                actorId,
                Action.BATCH_RUN_SKIPPED.name(),
                EntityType.BATCH.name(),
                first.runId(), // entity_id는 runId 아무거나 OK (깨진 row 용)
                "{ this is not json"
        );
        assertThat(inserted).isEqualTo(1);

        Long badSkipCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM audit_log
                WHERE action = ?
                    AND entity_type = ?
                    AND occurred_at >= ?
                    AND occurred_at < ?
                    AND actor_id = ?
                """, Long.class,
                Action.BATCH_RUN_SKIPPED.name(),
                EntityType.BATCH.name(),
                Timestamp.valueOf(from),
                Timestamp.valueOf(toExclusive),
                actorId
        );
        assertThat(badSkipCount).isNotNull();
        assertThat(badSkipCount).isGreaterThanOrEqualTo(1L);

        // when (깨진 SKIP가 있어도 조회가 안 깨져야 함)
        SettlementBatchHistoryResponse res = settlementBatchQueryService.getHistory(
                baseDate, baseDate, PageRequest.of(0, 20)
        );

        // then
        assertThat(res).isNotNull();
        assertThat(res.page()).isNotNull();
        assertThat(res.from()).isEqualTo(baseDate);
        assertThat(res.to()).isEqualTo(baseDate);

        // 핵심 계약: "깨진 SKIP가 있어도 조회 자체가 실패하지 않는다"
        // (row 포함 여부/개수는 UI merge 정책상 변동 가능 -> 여기선 강제하지 않음)
    }
}
