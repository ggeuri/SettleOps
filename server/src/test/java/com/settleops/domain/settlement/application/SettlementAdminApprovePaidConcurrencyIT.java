package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.entity.Settlement;
import com.settleops.domain.settlement.enums.SettlementStatus;
import com.settleops.domain.settlement.infra.SettlementRepository;
import com.settleops.global.audit.EntityType;
import com.settleops.global.enums.Action;
import com.settleops.global.error.BadRequestException;
import com.settleops.global.logging.RequestIdKeys;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SettlementAdminApprovePaidConcurrencyIT {

    @Autowired SettlementAdminCommandService settlementAdminCommandService;
    @Autowired SettlementRepository settlementRepository;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void approvePaid_concurrent_two_requests_should_write_two_audits_and_return_paid_for_both() throws Exception {
        String settlementId = UUID.randomUUID().toString(); // CHAR(36)
        String merchantId = "M1";
        Long batchId = 1L;
        LocalDate baseDate = LocalDate.now();

        seedPayRequestedCommitted(settlementId, merchantId, batchId, baseDate);
        assertThat(settlementRepository.findById(settlementId).orElseThrow().getStatus())
                .isEqualTo(SettlementStatus.PAY_REQUESTED);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CyclicBarrier barrier = new CyclicBarrier(2);

            Future<SettlementStatus> f1 = pool.submit(() ->
                    callApprovePaidWithThreadContext(barrier, settlementId, "approverA")
            );
            Future<SettlementStatus> f2 = pool.submit(() ->
                    callApprovePaidWithThreadContext(barrier, settlementId, "approverB")
            );

            SettlementStatus r1 = getOrFailFast(f1);
            SettlementStatus r2 = getOrFailFast(f2);

            // then: 둘 다 PAID 반환 (LOCKED no-op 200)
            assertThat(r1).isEqualTo(SettlementStatus.PAID);
            assertThat(r2).isEqualTo(SettlementStatus.PAID);

            // DB 최종값 재조회
            em.clear();
            Settlement saved = settlementRepository.findById(settlementId).orElseThrow();
            assertThat(saved.getStatus()).isEqualTo(SettlementStatus.PAID);
            assertThat(saved.getPaidApprovedAt()).isNotNull();
            assertThat(saved.getPaidApprovedBy()).isIn("approverA", "approverB");

            // audit_log 2건(각 스레드 1건) + actorId 2명 + requestId 중복 없음
            Long auditCount = jdbcTemplate.queryForObject(
                    """
                    select count(*)
                    from audit_log
                    where entity_type = ?
                      and entity_id = ?
                      and action = ?
                    """,
                    Long.class,
                    EntityType.SETTLEMENT.name(),
                    settlementId,
                    Action.SETTLEMENT_PAY_APPROVED.name()
            );
            assertThat(auditCount).isNotNull();
            assertThat(auditCount).isEqualTo(2L);

            List<String> actorIds = jdbcTemplate.queryForList(
                    """
                    select actor_id
                    from audit_log
                    where entity_type = ?
                      and entity_id = ?
                      and action = ?
                    """,
                    String.class,
                    EntityType.SETTLEMENT.name(),
                    settlementId,
                    Action.SETTLEMENT_PAY_APPROVED.name()
            );
            assertThat(actorIds).containsExactlyInAnyOrder("approverA", "approverB");

            List<String> requestIds = jdbcTemplate.queryForList(
                    """
                    select request_id
                    from audit_log
                    where entity_type = ?
                      and entity_id = ?
                      and action = ?
                    """,
                    String.class,
                    EntityType.SETTLEMENT.name(),
                    settlementId,
                    Action.SETTLEMENT_PAY_APPROVED.name()
            );
            assertThat(requestIds).doesNotHaveDuplicates();

        } finally {
            pool.shutdown();
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        }
    }

    @Test
    void approvePaid_invalidActorId_should_throw_400_BadRequestException_from_AuditLogger_validation() {
        String settlementId = UUID.randomUUID().toString();
        String merchantId = "M1";
        Long batchId = 1L;

        seedPayRequestedCommitted(settlementId, merchantId, batchId, LocalDate.now());

        MDC.put(RequestIdKeys.MDC_KEY, UUID.randomUUID().toString());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("bad actor", "N/A") // 공백 포함(ASCII/공백 금지 규칙 위반)
        );

        assertThatThrownBy(() -> settlementAdminCommandService.approvePaid(settlementId, "ok"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("actorId");
    }

    private SettlementStatus callApprovePaidWithThreadContext(
            CyclicBarrier barrier,
            String settlementId,
            String approverId
    ) throws Exception {
        try {
            MDC.put(RequestIdKeys.MDC_KEY, UUID.randomUUID().toString());
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(approverId, "N/A")
            );

            try {
                barrier.await(5, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                throw new AssertionError("BARRIER_TIMEOUT: other thread did not reach barrier in time", e);
            } catch (BrokenBarrierException e) {
                throw new AssertionError("BARRIER_BROKEN: barrier broken (other thread failed/timeout)", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("BARRIER_INTERRUPTED", e);
            }

            return settlementAdminCommandService.approvePaid(settlementId, "ok").status();
        } finally {
            MDC.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private SettlementStatus getOrFailFast(Future<SettlementStatus> f) throws Exception {
        try {
            return f.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new AssertionError("FUTURE_TIMEOUT", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw new AssertionError("TASK_FAILED: " + cause.getClass().getSimpleName() + " - " + cause.getMessage(), cause);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void seedPayRequestedCommitted(String settlementId, String merchantId, Long batchId, LocalDate baseDate) {
        Settlement s = Settlement.createReady(
                settlementId,
                "NO-" + UUID.randomUUID(),
                batchId,
                merchantId,
                baseDate,
                1000L,
                10L,
                1L,
                989L
        );

        settlementRepository.save(s);

        s.requestPaid("requesterX", LocalDateTime.now());
        settlementRepository.save(s);

        em.flush();
        em.clear();
    }
}