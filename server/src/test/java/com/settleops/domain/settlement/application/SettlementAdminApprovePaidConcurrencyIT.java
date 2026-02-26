package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.entity.Settlement;
import com.settleops.domain.settlement.enums.SettlementStatus;
import com.settleops.domain.settlement.infra.SettlementRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class SettlementAdminApprovePaidConcurrencyIT {

    @Autowired
    SettlementAdminCommandService settlementAdminCommandService;

    @Autowired
    SettlementRepository settlementRepository;

    @Autowired
    EntityManager em;

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void approvePaid_concurrent_two_requests_should_transition_paid_once_and_other_noop() throws Exception {
        String settlementId = "S-" + UUID.randomUUID();
        String merchantId = "M1";
        Long batchId = 1L;

        seedPayRequested(settlementId, merchantId, batchId);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        SettlementStatus r1;
        SettlementStatus r2;
        try {
            CyclicBarrier barrier = new CyclicBarrier(2);

            Future<SettlementStatus> f1 = pool.submit(() -> callApprovePaidWithThreadContext(barrier, settlementId, "approverA"));
            Future<SettlementStatus> f2 = pool.submit(() -> callApprovePaidWithThreadContext(barrier, settlementId, "approverB"));

            try {
                r1 = f1.get(10, TimeUnit.SECONDS);
                r2 = f2.get(10, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                throw new AssertionError("FUTURE_TIMEOUT: approvePaid did not finish in time", e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                throw new AssertionError("TASK_FAILED: " + cause.getClass().getSimpleName() + " - " + cause.getMessage(), cause);
            }

            // then: 둘 다 PAID 반환
            assertThat(r1).isEqualTo(SettlementStatus.PAID);
            assertThat(r2).isEqualTo(SettlementStatus.PAID);

            // 영속성 컨텍스트 캐시 영향 제거 + DB 최종값 재조회
            em.clear();  // 1차 캐시 비우기
            Settlement saved = settlementRepository.findById(settlementId).orElseThrow();
            assertThat(saved.getStatus()).isEqualTo(SettlementStatus.PAID);
            assertThat(saved.getPaidApprovedAt()).isNotNull();
            assertThat(saved.getPaidApprovedBy()).isIn("approverA", "approverB");
        } finally {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private SettlementStatus callApprovePaidWithThreadContext(CyclicBarrier barrier, String settlementId, String approverId) throws Exception {
        try {
            MDC.put("requestId", UUID.randomUUID().toString());
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(approverId, "N/A")
            );

            try {
                barrier.await(5, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                throw new AssertionError("BARRIER_TIMEOUT: other thread did not reach barrier in time", e);
            } catch (BrokenBarrierException e) {
                throw new AssertionError("BARRIER_BROKEN: barrier was broken (other thread failed/timeout/interrupted)", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("BARRIER_INTERRUPTED: thread interrupted while waiting at barrier", e);
            }

            return settlementAdminCommandService.approvePaid(settlementId, "ok").status();
        } finally {
            MDC.clear();
            SecurityContextHolder.clearContext();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void seedPayRequested(String settlementId, String merchantId, Long batchId) {
        Settlement s = Settlement.createReady(
                settlementId,
                "NO-" + UUID.randomUUID(),
                batchId,
                merchantId,
                LocalDate.now(),
                1000L,
                10L,
                1L,
                989L
        );

        settlementRepository.save(s);

        s.requestPaid("requesterX", LocalDateTime.now());
        settlementRepository.save(s);

        em.flush(); // REQUIRES_NEW 트랜잭션 안에서 확실히 DB 반영
        em.clear(); // seed로 만든 1차 캐시 제거
    }
}