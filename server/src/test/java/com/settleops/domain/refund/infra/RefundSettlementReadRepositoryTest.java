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
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RefundSettlementReadRepositoryTest {

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
    void APPROVED이고_link가_없고_cutoff_이전이면_입력집합에_포함된다() {
        String refundId = "11111111-1111-1111-1111-111111111111";

        em.persist(Refund.builder()
                .refundId(refundId)
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

        em.flush();
        em.clear();

        List<ApprovedRefundAdjustment> result =
                repository.findApprovedUnlinkedRefundAdjustmentsBefore(
                        LocalDateTime.of(2026, 3, 11, 0, 0)
                );

        assertThat(result).extracting(ApprovedRefundAdjustment::refundId)
                .containsExactly(refundId);
    }

    @Test
    void APPROVED라도_link가_있으면_입력집합에서_제외된다() {
        String refundId = "22222222-2222-2222-2222-222222222222";

        em.persist(Refund.builder()
                .refundId(refundId)
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
                refundId,
                "settlement-1",
                LocalDateTime.of(2026, 3, 11, 0, 30)
        ));

        em.flush();
        em.clear();

        List<ApprovedRefundAdjustment> result =
                repository.findApprovedUnlinkedRefundAdjustmentsBefore(
                        LocalDateTime.of(2026, 3, 11, 0, 0)
                );

        assertThat(result).isEmpty();
    }

    @Test
    void APPROVED상태만_다음배치_REFUND입력집합_대상이된다() {
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
                repository.findApprovedUnlinkedRefundAdjustmentsBefore(
                        LocalDateTime.of(2026, 3, 11, 0, 0)
                );

        assertThat(result).hasSize(1);
        assertThat(result).extracting(ApprovedRefundAdjustment::refundId)
                .containsExactly(refundApproved);
    }

    @Test
    void KST기준_cutoff는_lt이며_baseDate_당일_00시_승인건은_제외된다() {
        String includedRefundId = "10101010-1010-1010-1010-101010101010";
        String excludedRefundId = "20202020-2020-2020-2020-202020202020";

        em.persist(Refund.builder()
                .refundId(includedRefundId)
                .paymentId("pay-included")
                .merchantId("merchant1")
                .buyerId("buyer1")
                .amount(3000L)
                .currency("KRW")
                .status(RefundStatus.APPROVED)
                .reasonText("included")
                .requestedAt(LocalDateTime.of(2026, 3, 10, 20, 0))
                .decidedAt(LocalDateTime.of(2026, 3, 10, 23, 59, 59))
                .build());

        em.persist(Refund.builder()
                .refundId(excludedRefundId)
                .paymentId("pay-excluded")
                .merchantId("merchant1")
                .buyerId("buyer1")
                .amount(4000L)
                .currency("KRW")
                .status(RefundStatus.APPROVED)
                .reasonText("excluded")
                .requestedAt(LocalDateTime.of(2026, 3, 10, 21, 0))
                .decidedAt(LocalDateTime.of(2026, 3, 11, 0, 0))
                .build());

        em.flush();
        em.clear();

        List<ApprovedRefundAdjustment> result =
                repository.findApprovedUnlinkedRefundAdjustmentsBefore(
                        LocalDateTime.of(2026, 3, 11, 0, 0)
                );

        assertThat(result).extracting(ApprovedRefundAdjustment::refundId)
                .containsExactly(includedRefundId);
    }

    @Test
    void APPROVED라도_decidedAt이_null이면_입력집합에서_제외된다() {
        em.persist(Refund.builder()
                .refundId("99999999-9999-9999-9999-999999999999")
                .paymentId("pay-no-decided")
                .merchantId("merchant1")
                .buyerId("buyer1")
                .amount(1500L)
                .currency("KRW")
                .status(RefundStatus.APPROVED)
                .reasonText("invalid approved")
                .requestedAt(LocalDateTime.of(2026, 3, 10, 10, 0))
                .decidedAt(null)
                .build());

        em.flush();
        em.clear();

        List<ApprovedRefundAdjustment> result =
                repository.findApprovedUnlinkedRefundAdjustmentsBefore(
                        LocalDateTime.of(2026, 3, 11, 0, 0)
                );

        assertThat(result).isEmpty();
    }

    @Test
    void cutoff이전의_unlinked_APPROVED가_여러건이면_모두_입력집합에_포함된다() {
        String refund1 = "aaaa0000-0000-0000-0000-000000000001";
        String refund2 = "aaaa0000-0000-0000-0000-000000000002";

        em.persist(Refund.builder()
                .refundId(refund1)
                .paymentId("pay-1")
                .merchantId("merchant1")
                .buyerId("buyer1")
                .amount(1000L)
                .currency("KRW")
                .status(RefundStatus.APPROVED)
                .reasonText("approved1")
                .requestedAt(LocalDateTime.of(2026, 3, 10, 9, 0))
                .decidedAt(LocalDateTime.of(2026, 3, 10, 10, 0))
                .build());

        em.persist(Refund.builder()
                .refundId(refund2)
                .paymentId("pay-2")
                .merchantId("merchant1")
                .buyerId("buyer1")
                .amount(2000L)
                .currency("KRW")
                .status(RefundStatus.APPROVED)
                .reasonText("approved2")
                .requestedAt(LocalDateTime.of(2026, 3, 10, 11, 0))
                .decidedAt(LocalDateTime.of(2026, 3, 10, 12, 0))
                .build());

        em.flush();
        em.clear();

        List<ApprovedRefundAdjustment> result =
                repository.findApprovedUnlinkedRefundAdjustmentsBefore(
                        LocalDateTime.of(2026, 3, 11, 0, 0)
                );

        assertThat(result).extracting(ApprovedRefundAdjustment::refundId)
                .containsExactlyInAnyOrder(refund1, refund2);
    }
}