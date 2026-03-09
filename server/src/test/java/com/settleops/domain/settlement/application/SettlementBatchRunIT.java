package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.dto.SettlementBatchRunResponse;
import com.settleops.domain.settlement.infra.SettlementBatchRepository;
import com.settleops.global.logging.RequestIdKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class SettlementBatchRunIT {

    @Autowired SettlementAdminCommandService settlementAdminCommandService;
    @Autowired SettlementBatchRepository settlementBatchRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void runBatch_sameBaseDate_twice_should_skip_second_time() {
        // 재현성 고정(자정/타임존 흔들림 제거)
        LocalDate baseDate = LocalDate.of(2026, 3, 2);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("adminA", "N/A")
        );
        SettlementBatchRunResponse r1 = settlementAdminCommandService.runBatch(baseDate, "req-test-001");
        assertThat(r1.runId()).isNotBlank();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("adminA", "N/A")
        );
        SettlementBatchRunResponse r2 = settlementAdminCommandService.runBatch(baseDate, "req-test-002");
        assertThat(r2.runId()).isNotBlank();

        // 첫 실행은 처리 결과(OK/FAIL), 두 번째는 무조건 SKIP
        assertThat(r1.result()).isIn(
                SettlementBatchRunResponse.RunResult.OK,
                SettlementBatchRunResponse.RunResult.FAIL
        );
        assertThat(r2.result()).isEqualTo(SettlementBatchRunResponse.RunResult.SKIP);

        assertThat(settlementBatchRepository.findByBatchKey(baseDate)).isPresent();
    }
}