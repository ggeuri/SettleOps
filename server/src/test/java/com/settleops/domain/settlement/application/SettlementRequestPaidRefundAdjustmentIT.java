package com.settleops.domain.settlement.application;

import com.settleops.domain.refund.domain.Refund;
import com.settleops.domain.refund.domain.RefundSettlementLink;
import com.settleops.domain.refund.domain.RefundStatus;
import com.settleops.domain.refund.infra.RefundRepository;
import com.settleops.domain.refund.infra.RefundSettlementLinkRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SettlementRequestPaidRefundAdjustmentIT {

    @Autowired SettlementAdminCommandService settlementAdminCommandService;
    @Autowired SettlementRepository settlementRepository;
    @Autowired SettlementBatchRepository settlementBatchRepository;
    @Autowired SettlementLineRepository settlementLineRepository;
    @Autowired RefundRepository refundRepository;
    @Autowired RefundSettlementLinkRepository refundSettlementLinkRepository;
    @Autowired TransactionTemplate tx;
    @Autowired EntityManager em;
    @Autowired RefundAdjustmentPolicy refundAdjustmentPolicy;

    @BeforeEach
    void setUp() {
        cleanup();
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        SecurityContextHolder.clearContext();
        refundSettlementLinkRepository.deleteAll();
        refundRepository.deleteAll();
        settlementLineRepository.deleteAll();
        settlementRepository.deleteAll();
        settlementBatchRepository.deleteAll();
    }

    @Test
    void refundAdjustmentPolicy는_실제구현이_주입된다() {
        assertThat(refundAdjustmentPolicy).isNotNull();
        assertThat(AopUtils.getTargetClass(refundAdjustmentPolicy).getSimpleName())
                .isEqualTo("RefundAdjustmentPolicyImpl");
    }

    @Test
    void 승인환불이_미반영이면_requestPaid는_409_REFUND_ADJUSTMENT_PENDING() {
        String settlementId = UUID.randomUUID().toString();
        String paymentId = UUID.randomUUID().toString();
        String refundId = UUID.randomUUID().toString();

        seedReadySettlementWithApprovedRefund(
                settlementId,
                paymentId,
                refundId,
                false,
                LocalDate.of(2026, 3, 12)
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin-requester", "N/A")
        );

        assertThatThrownBy(() ->
                settlementAdminCommandService.requestPaid(
                        settlementId,
                        "request paid",
                        UUID.randomUUID().toString()
                )
        ).isInstanceOf(ConflictException.class)
                .satisfies(ex -> {
                    ConflictException ce = (ConflictException) ex;
                    assertThat(ce.toErrorResponse().reason())
                            .isEqualTo(ReasonCode.REFUND_ADJUSTMENT_PENDING.name());
                });
    }

    @Test
    void 승인환불이_이미_link로_반영됐으면_requestPaid_정상진행() {
        String settlementId = UUID.randomUUID().toString();
        String paymentId = UUID.randomUUID().toString();
        String refundId = UUID.randomUUID().toString();

        seedReadySettlementWithApprovedRefund(
                settlementId,
                paymentId,
                refundId,
                true,
                LocalDate.of(2026, 3, 13)
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin-requester", "N/A")
        );

        SettlementPayActionResponse response = settlementAdminCommandService.requestPaid(
                settlementId,
                "request paid",
                UUID.randomUUID().toString()
        );

        assertThat(response.status()).isEqualTo(SettlementStatus.PAY_REQUESTED);
        assertThat(response.paidRequestedAt()).isNotNull();

        em.clear();
        Settlement saved = settlementRepository.findById(settlementId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(SettlementStatus.PAY_REQUESTED);
    }

    private void seedReadySettlementWithApprovedRefund(
            String settlementId,
            String paymentId,
            String refundId,
            boolean linked,
            LocalDate baseDate
    ) {
        tx.executeWithoutResult(status -> {
            SettlementBatch batch = settlementBatchRepository.save(
                    SettlementBatch.started(
                            baseDate,
                            UUID.randomUUID().toString(),
                            "ADMIN:seed",
                            UUID.randomUUID().toString()
                    )
            );

            batch.markOk(baseDate.atTime(1, 0));

            Settlement settlement = Settlement.createReady(
                    settlementId,
                    "SET-" + baseDate + "-M1",
                    batch.getBatchId(),
                    "M1",
                    baseDate,
                    10000L,
                    0L,
                    0L,
                    10000L
            );
            settlementRepository.save(settlement);

            settlementLineRepository.save(
                    SettlementLine.of(
                            settlementId,
                            paymentId,
                            SettlementLineType.PAYMENT,
                            10000L
                    )
            );

            refundRepository.save(
                    Refund.builder()
                            .refundId(refundId)
                            .paymentId(paymentId)
                            .merchantId("M1")
                            .buyerId("B1")
                            .amount(3000L)
                            .currency("KRW")
                            .status(RefundStatus.APPROVED)
                            .reasonText("approved refund")
                            .requestedAt(baseDate.minusDays(1).atTime(10, 0))
                            .decidedAt(baseDate.minusDays(1).atTime(11, 0))
                            .build()
            );

            if (linked) {
                refundSettlementLinkRepository.save(
                        RefundSettlementLink.of(
                                refundId,
                                settlementId,
                                baseDate.atTime(1, 5)
                        )
                );
            }

            em.flush();
            em.clear();
        });
    }
}