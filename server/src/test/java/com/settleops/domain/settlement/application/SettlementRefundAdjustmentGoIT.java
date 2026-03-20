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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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

    private static final String ADMIN_ID = "adminA";

    @Autowired
    SettlementAdminCommandService settlementAdminCommandService;

    @Autowired
    SettlementRepository settlementRepository;

    @Autowired
    SettlementBatchRepository settlementBatchRepository;

    @Autowired
    SettlementLineRepository settlementLineRepository;

    @Autowired
    EntityManager em;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    RefundAdjustmentPolicy refundAdjustmentPolicy;

    @Autowired
    RefundSettlementLinkRepository refundSettlementLinkRepository;

    @Test
    @DisplayName("GO: APPROVED refund 존재 시 request-paid 차단되고, 다음 batch에서 REFUND line/link 반영 후 차단이 해제된다")
    void approved_refund_blocks_request_paid_then_next_batch_applies_refund_line_and_link_and_unblocks() {
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

        boolean pendingForBlockedSettlementBeforeBatch =
                refundAdjustmentPolicy.isRefundAdjustmentPending(blockedSettlementId);

        boolean hasLinkForBlockedSettlementBeforeBatch =
                refundSettlementLinkRepository.existsBySettlementId(blockedSettlementId);

        assertThat(pendingForBlockedSettlementBeforeBatch).isTrue();
        assertThat(hasLinkForBlockedSettlementBeforeBatch).isFalse();

        assertThatThrownBy(() ->
                settlementAdminCommandService.requestPaid(
                        blockedSettlementId,
                        "before next batch",
                        "req-go-block-001",
                        ADMIN_ID
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

        SettlementBatchRunResponse batchResponse =
                settlementAdminCommandService.runBatch(nextBatchBaseDate, "req-go-batch-001", ADMIN_ID);

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

        Long refundLineCountForApprovedOldPayment = jdbcTemplate.queryForObject(
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

        assertThat(refundLineCountForApprovedOldPayment).isEqualTo(1L);

        Long refundLinkCountForApprovedRefund = jdbcTemplate.queryForObject(
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

        assertThat(refundLinkCountForApprovedRefund).isEqualTo(1L);

        boolean pendingForNewSettlementAfterBatch =
                refundAdjustmentPolicy.isRefundAdjustmentPending(newSettlementId);

        boolean hasLinkForNewSettlementAfterBatch =
                refundSettlementLinkRepository.existsBySettlementId(newSettlementId);

        assertThat(pendingForNewSettlementAfterBatch).isFalse();
        assertThat(hasLinkForNewSettlementAfterBatch).isTrue();

        SettlementPayActionResponse newSettlementPayRequestResponse =
                settlementAdminCommandService.requestPaid(
                        newSettlementId,
                        "after next batch on newly created settlement",
                        "req-go-request-paid-new-001",
                        ADMIN_ID
                );

        assertThat(newSettlementPayRequestResponse.status()).isEqualTo(SettlementStatus.PAY_REQUESTED);

        em.clear();
        Settlement newSettlementSaved = settlementRepository.findById(newSettlementId).orElseThrow();
        assertThat(newSettlementSaved.getStatus()).isEqualTo(SettlementStatus.PAY_REQUESTED);
        assertThat(newSettlementSaved.getPaidRequestedAt()).isNotNull();

        boolean pendingForBlockedSettlementAfterBatch =
                refundAdjustmentPolicy.isRefundAdjustmentPending(blockedSettlementId);

        boolean hasLinkForBlockedSettlementAfterBatch =
                refundSettlementLinkRepository.existsBySettlementId(blockedSettlementId);

        assertThat(pendingForBlockedSettlementAfterBatch).isFalse();
        assertThat(hasLinkForBlockedSettlementAfterBatch).isFalse();

        SettlementPayActionResponse blockedSettlementPayRequestResponse =
                settlementAdminCommandService.requestPaid(
                        blockedSettlementId,
                        "after next batch on originally blocked settlement",
                        "req-go-request-paid-old-001",
                        ADMIN_ID
                );

        assertThat(blockedSettlementPayRequestResponse.status()).isEqualTo(SettlementStatus.PAY_REQUESTED);

        em.clear();
        Settlement blockedSettlementSaved = settlementRepository.findById(blockedSettlementId).orElseThrow();
        assertThat(blockedSettlementSaved.getStatus()).isEqualTo(SettlementStatus.PAY_REQUESTED);
        assertThat(blockedSettlementSaved.getPaidRequestedAt()).isNotNull();
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