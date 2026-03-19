package com.settleops.domain.settlement.application;

import com.settleops.domain.refund.domain.Refund;
import com.settleops.domain.refund.domain.RefundStatus;
import com.settleops.domain.refund.infra.RefundSettlementLinkRepository;
import com.settleops.domain.settlement.dto.SettlementBatchRunResponse;
import com.settleops.domain.settlement.dto.SettlementPayActionResponse;
import com.settleops.domain.settlement.entity.Settlement;
import com.settleops.domain.settlement.entity.SettlementBatch;
import com.settleops.domain.settlement.entity.SettlementLine;
import com.settleops.domain.settlement.enums.SettlementLineType;
import com.settleops.domain.settlement.enums.SettlementStatus;
import com.settleops.domain.settlement.infra.SettlementBatchRepository;
import com.settleops.domain.settlement.infra.SettlementLineRepository;
import com.settleops.domain.settlement.infra.SettlementRepository;
import com.settleops.global.enums.ReasonCode;
import com.settleops.global.error.ConflictException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SettlementRefundAdjustmentGoIT {

    @Autowired SettlementAdminCommandService settlementAdminCommandService;
    @Autowired SettlementRepository settlementRepository;
    @Autowired SettlementBatchRepository settlementBatchRepository;
    @Autowired SettlementLineRepository settlementLineRepository;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired TransactionTemplate tx;

    @Autowired RefundAdjustmentPolicy refundAdjustmentPolicy;
    @Autowired RefundSettlementLinkRepository refundSettlementLinkRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GO: APPROVED refund -> next batch REFUND line/link 생성 -> request-paid 차단 해제")
    void approved_refund_next_batch_refund_line_and_link_then_request_paid_unblocked() {
        String merchantId = "M-GO-" + UUID.randomUUID().toString().substring(0, 8);
        String buyerId = "B-GO-1";

        LocalDate blockedSettlementBaseDate = LocalDate.of(2026, 3, 10);
        LocalDate nextBatchBaseDate = LocalDate.of(2026, 3, 12);

        String oldPaymentId = UUID.randomUUID().toString();
        String refundId = UUID.randomUUID().toString();

        String blockedSettlementId = seedReadySettlementWithPaymentLine(
                blockedSettlementBaseDate,
                merchantId,
                oldPaymentId,
                10000L
        );

        seedApprovedRefund(
                refundId,
                oldPaymentId,
                merchantId,
                buyerId,
                3000L,
                LocalDateTime.of(2026, 3, 11, 9, 0),
                LocalDateTime.of(2026, 3, 11, 10, 0)
        );

        boolean pendingBeforeNextBatch =
                refundAdjustmentPolicy.isRefundAdjustmentPending(blockedSettlementId);

        boolean hasLinkBeforeNextBatch =
                refundSettlementLinkRepository.existsBySettlementId(blockedSettlementId);

        assertThat(pendingBeforeNextBatch).isTrue();
        assertThat(hasLinkBeforeNextBatch).isFalse();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("adminA", "N/A")
        );

        assertThatThrownBy(() ->
                settlementAdminCommandService.requestPaid(
                        blockedSettlementId,
                        "before next batch",
                        "req-go-block-001"
                )
        ).isInstanceOf(ConflictException.class)
                .satisfies(ex -> {
                    ConflictException ce = (ConflictException) ex;
                    assertThat(ce.toErrorResponse().reason())
                            .isEqualTo(ReasonCode.REFUND_ADJUSTMENT_PENDING.name());
                });

        String newPaymentId = UUID.randomUUID().toString();
        seedConfirmedPaymentForBatch(
                newPaymentId,
                merchantId,
                buyerId,
                10000L,
                LocalDateTime.of(2026, 3, 12, 14, 0)
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("adminA", "N/A")
        );

        SettlementBatchRunResponse batchResponse =
                settlementAdminCommandService.runBatch(nextBatchBaseDate, "req-go-batch-001");

        assertThat(batchResponse.result()).isEqualTo(SettlementBatchRunResponse.RunResult.OK);

        String newSettlementId = jdbcTemplate.queryForObject(
                """
                select settlement_id
                from settlement
                where base_date = ?
                  and merchant_id = ?
                """,
                String.class,
                nextBatchBaseDate,
                merchantId
        );

        assertThat(newSettlementId).isNotBlank();

        Long refundLineCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from settlement_line
                where settlement_id = ?
                  and line_type = 'REFUND'
                  and payment_id = ?
                  and amount = ?
                """,
                Long.class,
                newSettlementId,
                oldPaymentId,
                3000L
        );

        assertThat(refundLineCount).isEqualTo(1L);

        Long refundLinkCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from refund_settlement_link
                where refund_id = ?
                  and settlement_id = ?
                """,
                Long.class,
                refundId,
                newSettlementId
        );

        assertThat(refundLinkCount).isEqualTo(1L);

        boolean pendingAfterNextBatch =
                refundAdjustmentPolicy.isRefundAdjustmentPending(newSettlementId);

        boolean hasLinkAfterNextBatch =
                refundSettlementLinkRepository.existsBySettlementId(newSettlementId);

        assertThat(pendingAfterNextBatch).isFalse();
        assertThat(hasLinkAfterNextBatch).isTrue();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("adminA", "N/A")
        );

        SettlementPayActionResponse payRequestResponse =
                settlementAdminCommandService.requestPaid(
                        newSettlementId,
                        "after next batch",
                        "req-go-request-paid-001"
                );

        assertThat(payRequestResponse.status()).isEqualTo(SettlementStatus.PAY_REQUESTED);

        em.clear();
        Settlement saved = settlementRepository.findById(newSettlementId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(SettlementStatus.PAY_REQUESTED);
        assertThat(saved.getPaidRequestedAt()).isNotNull();
    }

    private String seedReadySettlementWithPaymentLine(
            LocalDate baseDate,
            String merchantId,
            String paymentId,
            long amount
    ) {
        return tx.execute(status -> {
            Long batchId = createBatchAndReturnId(baseDate);

            String settlementId = UUID.randomUUID().toString();
            Settlement settlement = Settlement.createReady(
                    settlementId,
                    "SET-" + merchantId + "-" + baseDate,
                    batchId,
                    merchantId,
                    baseDate,
                    amount,
                    0L,
                    0L,
                    amount
            );
            settlementRepository.save(settlement);

            SettlementLine paymentLine = SettlementLine.of(
                    settlementId,
                    paymentId,
                    SettlementLineType.PAYMENT,
                    amount
            );
            settlementLineRepository.save(paymentLine);

            em.flush();
            em.clear();

            return settlementId;
        });
    }

    private void seedApprovedRefund(
            String refundId,
            String paymentId,
            String merchantId,
            String buyerId,
            long amount,
            LocalDateTime requestedAt,
            LocalDateTime decidedAt
    ) {
        tx.executeWithoutResult(status -> {
            Refund refund = Refund.builder()
                    .refundId(refundId)
                    .paymentId(paymentId)
                    .merchantId(merchantId)
                    .buyerId(buyerId)
                    .amount(amount)
                    .currency("KRW")
                    .status(RefundStatus.APPROVED)
                    .reasonText("GO_TEST")
                    .requestedAt(requestedAt)
                    .decidedAt(decidedAt)
                    .build();

            em.persist(refund);
            em.flush();
            em.clear();
        });
    }

    /**
     * 주의:
     * payment_event 컬럼명은 현재 DDL 기준으로 맞춰야 한다.
     * 아래 insert는 지금까지 대화 기준 가장 가능성 높은 형태로 작성했다.
     * 만약 payment_event에 필수 컬럼이 더 있으면 그 컬럼만 추가하면 된다.
     */
    private void seedConfirmedPaymentForBatch(
            String paymentId,
            String merchantId,
            String buyerId,
            long amount,
            LocalDateTime confirmedOccurredAt
    ) {
        tx.executeWithoutResult(status -> {
            String orderId = UUID.randomUUID().toString();
            LocalDateTime createdAt = confirmedOccurredAt.minusHours(1);

            jdbcTemplate.update(
                    """
                    insert into payment
                    (payment_id, order_id, merchant_id, buyer_id, currency,
                     requested_amount, captured_amount, status, created_at, updated_at)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    paymentId,
                    orderId,
                    merchantId,
                    buyerId,
                    "KRW",
                    amount,
                    amount,
                    "CAPTURED",
                    createdAt,
                    createdAt
            );

            jdbcTemplate.update(
                    """
                    insert into payment_event
                    (payment_id, event_type, request_id, occurred_at)
                    values (?, ?, ?, ?)
                    """,
                    paymentId,
                    "PAYMENT_CONFIRMED",
                    UUID.randomUUID().toString(),
                    confirmedOccurredAt
            );
        });
    }

    private Long createBatchAndReturnId(LocalDate baseDate) {
        return tx.execute(status -> {
            SettlementBatch batch = SettlementBatch.started(
                    baseDate,
                    UUID.randomUUID().toString(),
                    "ADMIN:test",
                    UUID.randomUUID().toString()
            );
            SettlementBatch saved = settlementBatchRepository.save(batch);
            em.flush();
            em.clear();
            return saved.getBatchId();
        });
    }
}