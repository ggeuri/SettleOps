package com.settleops.domain.refund.infra;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.settleops.domain.refund.application.dto.ApprovedRefundAdjustment;
import com.settleops.domain.refund.domain.Refund;
import com.settleops.domain.refund.domain.RefundSettlementLink;
import com.settleops.domain.refund.domain.RefundStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test-db")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)class RefundSettlementReadRepositoryTest {

    @jakarta.annotation.Resource
    private EntityManager em;

    @jakarta.annotation.Resource
    private RefundSettlementReadRepository repository;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        JPAQueryFactory jpaQueryFactory(EntityManager em) {
            return new JPAQueryFactory(em);
        }

        @org.springframework.context.annotation.Bean
        RefundSettlementReadRepository refundSettlementReadRepository(JPAQueryFactory queryFactory) {
            return new RefundSettlementReadRepository(queryFactory);
        }
    }

    @Test
    void approved이면서_link가_없고_cutoff이전인_refund만_조회된다() {
        String refund1 = "11111111-1111-1111-1111-111111111111";
        String refund2 = "22222222-2222-2222-2222-222222222222";
        String refund3 = "33333333-3333-3333-3333-333333333333";

        em.persist(Refund.builder()
                .refundId(refund1)
                .paymentId("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
                .merchantId("merchant1")
                .buyerId("buyer1")
                .amount(3000L)
                .currency("KRW")
                .status(RefundStatus.APPROVED)
                .reasonText("ok")
                .requestedAt(LocalDateTime.of(2026, 3, 10, 10, 0))
                .decidedAt(LocalDateTime.of(2026, 3, 10, 11, 0))
                .build());

        em.persist(Refund.builder()
                .refundId(refund2)
                .paymentId("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
                .merchantId("merchant1")
                .buyerId("buyer1")
                .amount(2000L)
                .currency("KRW")
                .status(RefundStatus.APPROVED)
                .reasonText("linked")
                .requestedAt(LocalDateTime.of(2026, 3, 10, 12, 0))
                .decidedAt(LocalDateTime.of(2026, 3, 10, 13, 0))
                .build());

        em.persist(RefundSettlementLink.of(
                refund2,
                "settlement-1",
                LocalDateTime.of(2026, 3, 11, 0, 30)
        ));

        em.persist(Refund.builder()
                .refundId(refund3)
                .paymentId("cccccccc-cccc-cccc-cccc-cccccccccccc")
                .merchantId("merchant1")
                .buyerId("buyer1")
                .amount(1000L)
                .currency("KRW")
                .status(RefundStatus.APPROVED)
                .reasonText("same day")
                .requestedAt(LocalDateTime.of(2026, 3, 11, 1, 0))
                .decidedAt(LocalDateTime.of(2026, 3, 11, 1, 30))
                .build());

        em.flush();
        em.clear();

        List<ApprovedRefundAdjustment> result =
                repository.findApprovedRefundsWithoutSettlementLinkBefore(
                        LocalDateTime.of(2026, 3, 11, 0, 0)
                );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).refundId()).isEqualTo(refund1);
    }

    @Test
    void approved_status만_refund_adjustment_대상이된다() {
        String refundRequested = "aaaa1111-1111-1111-1111-111111111111";
        String refundRejected = "bbbb2222-2222-2222-2222-222222222222";
        String refundApproved = "cccc3333-3333-3333-3333-333333333333";

        // REQUESTED
        em.persist(Refund.builder()
                .refundId(refundRequested)
                .paymentId("pay-req-1")
                .merchantId("merchant1")
                .buyerId("buyer1")
                .amount(1000L)
                .currency("KRW")
                .status(RefundStatus.REQUESTED)
                .reasonText("requested")
                .requestedAt(LocalDateTime.of(2026, 3, 10, 9, 0))
                .decidedAt(null)
                .build());

        // REJECTED
        em.persist(Refund.builder()
                .refundId(refundRejected)
                .paymentId("pay-rej-1")
                .merchantId("merchant1")
                .buyerId("buyer1")
                .amount(2000L)
                .currency("KRW")
                .status(RefundStatus.REJECTED)
                .reasonText("rejected")
                .requestedAt(LocalDateTime.of(2026, 3, 10, 9, 30))
                .decidedAt(LocalDateTime.of(2026, 3, 10, 10, 0))
                .build());

        // APPROVED (정상 대상)
        em.persist(Refund.builder()
                .refundId(refundApproved)
                .paymentId("pay-app-1")
                .merchantId("merchant1")
                .buyerId("buyer1")
                .amount(3000L)
                .currency("KRW")
                .status(RefundStatus.APPROVED)
                .reasonText("approved")
                .requestedAt(LocalDateTime.of(2026, 3, 10, 10, 0))
                .decidedAt(LocalDateTime.of(2026, 3, 10, 11, 0))
                .build());

        em.flush();
        em.clear();

        List<ApprovedRefundAdjustment> result =
                repository.findApprovedRefundsWithoutSettlementLinkBefore(
                        LocalDateTime.of(2026, 3, 11, 0, 0)
                );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).refundId()).isEqualTo(refundApproved);
    }
    @Test
    void refundId에_대한_link가_있으면_true() {
        String refundId = "77777777-7777-7777-7777-777777777777";

        em.persist(Refund.builder()
                .refundId(refundId)
                .paymentId("pay-7777")
                .merchantId("merchant1")
                .buyerId("buyer1")
                .amount(1000L)
                .currency("KRW")
                .status(RefundStatus.APPROVED)
                .reasonText("approved")
                .requestedAt(LocalDateTime.of(2026, 3, 10, 10, 0))
                .decidedAt(LocalDateTime.of(2026, 3, 10, 11, 0))
                .build());

        em.persist(RefundSettlementLink.of(
                refundId,
                "settlement-777",
                LocalDateTime.of(2026, 3, 11, 0, 0)
        ));

        em.flush();
        em.clear();

        boolean result =
                repository.existsSettlementLinkByRefundId(refundId);

        assertThat(result).isTrue();
    }
    @Test
    void refundId에_대한_link가_없으면_false() {
        String refundId = "88888888-8888-8888-8888-888888888888";

        em.persist(Refund.builder()
                .refundId(refundId)
                .paymentId("pay-8888")
                .merchantId("merchant1")
                .buyerId("buyer1")
                .amount(2000L)
                .currency("KRW")
                .status(RefundStatus.APPROVED)
                .reasonText("approved")
                .requestedAt(LocalDateTime.of(2026, 3, 10, 10, 0))
                .decidedAt(LocalDateTime.of(2026, 3, 10, 11, 0))
                .build());

        em.flush();
        em.clear();

        boolean result =
                repository.existsSettlementLinkByRefundId(refundId);

        assertThat(result).isFalse();
    }
}