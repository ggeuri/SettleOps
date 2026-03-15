package com.settleops.domain.refund.infra;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.settleops.domain.refund.domain.Refund;
import com.settleops.domain.refund.domain.RefundSettlementLink;
import com.settleops.domain.refund.domain.RefundStatus;
import com.settleops.domain.settlement.entity.SettlementLine;
import com.settleops.domain.settlement.enums.SettlementLineType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RefundAdjustmentPendingReadRepositoryTest {

    @jakarta.annotation.Resource
    private EntityManager em;

    @jakarta.annotation.Resource
    private RefundAdjustmentPendingReadRepository repository;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        JPAQueryFactory jpaQueryFactory(EntityManager em) {
            return new JPAQueryFactory(em);
        }

        @org.springframework.context.annotation.Bean
        RefundAdjustmentPendingReadRepository refundAdjustmentPendingReadRepository(JPAQueryFactory queryFactory) {
            return new RefundAdjustmentPendingReadRepository(queryFactory);
        }
    }

    @Test
    void APPROVED이고_link가_없으면_true() {
        String settlementId = "11111111-1111-1111-1111-111111111111";
        String paymentId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
        String refundId = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

        em.persist(SettlementLine.of(
                settlementId,
                paymentId,
                SettlementLineType.PAYMENT,
                10000L
        ));

        em.persist(Refund.builder()
                .refundId(refundId)
                .paymentId(paymentId)
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

        boolean result = repository.existsPendingApprovedRefundForSettlement(settlementId);

        assertThat(result).isTrue();
    }

    @Test
    void APPROVED라도_link가_있으면_false() {
        String settlementId = "22222222-2222-2222-2222-222222222222";
        String paymentId = "cccccccc-cccc-cccc-cccc-cccccccccccc";
        String refundId = "dddddddd-dddd-dddd-dddd-dddddddddddd";

        em.persist(SettlementLine.of(
                settlementId,
                paymentId,
                SettlementLineType.PAYMENT,
                10000L
        ));

        em.persist(Refund.builder()
                .refundId(refundId)
                .paymentId(paymentId)
                .merchantId("merchant1")
                .buyerId("buyer1")
                .amount(4000L)
                .currency("KRW")
                .status(RefundStatus.APPROVED)
                .reasonText("approved")
                .requestedAt(LocalDateTime.of(2026, 3, 10, 10, 0))
                .decidedAt(LocalDateTime.of(2026, 3, 10, 11, 0))
                .build());

        em.persist(RefundSettlementLink.of(
                refundId,
                settlementId,
                LocalDateTime.of(2026, 3, 11, 0, 30)
        ));

        em.flush();
        em.clear();

        boolean result = repository.existsPendingApprovedRefundForSettlement(settlementId);

        assertThat(result).isFalse();
    }

    @Test
    void 같은_settlement에_하나는_link있고_하나는_link없으면_true() {
        String settlementId = "33333333-3333-3333-3333-333333333333";

        String paymentId1 = "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee";
        String paymentId2 = "ffffffff-ffff-ffff-ffff-ffffffffffff";

        String refundId1 = "12121212-1212-1212-1212-121212121212";
        String refundId2 = "34343434-3434-3434-3434-343434343434";

        em.persist(SettlementLine.of(
                settlementId,
                paymentId1,
                SettlementLineType.PAYMENT,
                10000L
        ));
        em.persist(SettlementLine.of(
                settlementId,
                paymentId2,
                SettlementLineType.PAYMENT,
                20000L
        ));

        em.persist(Refund.builder()
                .refundId(refundId1)
                .paymentId(paymentId1)
                .merchantId("merchant1")
                .buyerId("buyer1")
                .amount(3000L)
                .currency("KRW")
                .status(RefundStatus.APPROVED)
                .reasonText("linked refund")
                .requestedAt(LocalDateTime.of(2026, 3, 10, 9, 0))
                .decidedAt(LocalDateTime.of(2026, 3, 10, 10, 0))
                .build());

        em.persist(RefundSettlementLink.of(
                refundId1,
                settlementId,
                LocalDateTime.of(2026, 3, 11, 0, 10)
        ));

        em.persist(Refund.builder()
                .refundId(refundId2)
                .paymentId(paymentId2)
                .merchantId("merchant1")
                .buyerId("buyer1")
                .amount(5000L)
                .currency("KRW")
                .status(RefundStatus.APPROVED)
                .reasonText("pending refund")
                .requestedAt(LocalDateTime.of(2026, 3, 10, 11, 0))
                .decidedAt(LocalDateTime.of(2026, 3, 10, 12, 0))
                .build());

        em.flush();
        em.clear();

        boolean result = repository.existsPendingApprovedRefundForSettlement(settlementId);

        assertThat(result).isTrue();
    }

    @Test
    void APPROVED가_아니면_false() {
        String settlementId = "44444444-4444-4444-4444-444444444444";
        String paymentId = "abababab-abab-abab-abab-abababababab";
        String refundId = "cdcdcdcd-cdcd-cdcd-cdcd-cdcdcdcdcdcd";

        em.persist(SettlementLine.of(
                settlementId,
                paymentId,
                SettlementLineType.PAYMENT,
                10000L
        ));

        em.persist(Refund.builder()
                .refundId(refundId)
                .paymentId(paymentId)
                .merchantId("merchant1")
                .buyerId("buyer1")
                .amount(3000L)
                .currency("KRW")
                .status(RefundStatus.REQUESTED)
                .reasonText("not approved yet")
                .requestedAt(LocalDateTime.of(2026, 3, 10, 10, 0))
                .decidedAt(null)
                .build());

        em.flush();
        em.clear();

        boolean result = repository.existsPendingApprovedRefundForSettlement(settlementId);

        assertThat(result).isFalse();
    }
    @Test
    void PAYMENT_line이_아니면_대상_settlement로_보지_않는다() {
        String settlementId = "55555555-5555-5555-5555-555555555555";
        String paymentId = "eeeeeeee-1111-2222-3333-444444444444";
        String refundId = "ffffffff-1111-2222-3333-444444444444";

        em.persist(SettlementLine.of(
                settlementId,
                paymentId,
                SettlementLineType.REFUND,
                3000L
        ));

        em.persist(Refund.builder()
                .refundId(refundId)
                .paymentId(paymentId)
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

        boolean result = repository.existsPendingApprovedRefundForSettlement(settlementId);

        assertThat(result).isFalse();
    }
}