package com.settleops.domain.refund.infra;
import com.settleops.domain.refund.domain.Refund;
import com.settleops.domain.refund.domain.RefundStatus;
import com.settleops.domain.settlement.entity.SettlementLine;
import com.settleops.domain.settlement.enums.SettlementLineType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({com.settleops.global.config.QuerydslConfig.class, RefundReadRepositoryIT.TestConfig.class})
@Rollback(true) // 테스트 끝나면 true로 수정
class RefundReadRepositoryIT {

    @Autowired EntityManager em;
    @Autowired RefundReadRepository refundReadRepository;
    @Autowired RefundSettlementLinkRepository refundSettlementLinkRepository;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        RefundReadRepository refundReadRepository(com.querydsl.jpa.impl.JPAQueryFactory queryFactory) {
            return new RefundReadRepository(queryFactory);
        }
    }
    @Test
    void settlementId_기준_approvedRefund존재_및_link존재_육안검증() {
        // =====================
        // 1) 테스트용 ID 준비
        // =====================
        String settlementId = UUID.randomUUID().toString();
        String paymentId = UUID.randomUUID().toString();
        String refundId = UUID.randomUUID().toString();

        // =====================
        // 2) settlement_line(PAYMENT) 1줄 넣기
        //    -> "이 settlement에 payment가 포함된다"를 만들어줌
        // =====================
        em.persist(SettlementLine.of(
                settlementId,
                paymentId,
                SettlementLineType.PAYMENT,
                10000L
        ));

        // =====================
        // 3) refund(APPROVED) 1줄 넣기
        //    -> "이 payment에 승인된 환불이 있다"를 만들어줌
        // =====================
        em.persist(Refund.builder()
                .refundId(refundId)
                .paymentId(paymentId)
                .merchantId("m1")
                .buyerId("b1")
                .amount(3000L)
                .currency("KRW")
                .status(RefundStatus.APPROVED)
                .reasonText("test")
                .requestedAt(LocalDateTime.now().minusMinutes(10))
                .decidedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        em.flush();
        em.clear();

        // =====================
        // 4) (2번) approved refund 존재 여부 확인
        // =====================
        boolean hasApproved = refundReadRepository.existsApprovedRefundBySettlementIdForDebug(settlementId);

        // ✅ 여기서 true면 성공
        assertThat(hasApproved).isTrue();

        // =====================
        // 5) (1번) link 존재 여부 확인 (아직 link 안 넣었으니 false가 정상)
        // =====================
        boolean hasLinkBefore = refundSettlementLinkRepository.existsBySettlementId(settlementId);
        assertThat(hasLinkBefore).isFalse();
        System.out.println("hasLinkBefore = " + hasLinkBefore);

        // =====================
        // 6) refund_settlement_link에 "증거" 1줄 넣기
        //    -> 이제 link가 존재해야 함
        // =====================
        em.createNativeQuery("""
            INSERT INTO refund_settlement_link(refund_id, settlement_id, created_at)
            VALUES (?, ?, NOW())
        """)
                .setParameter(1, refundId)
                .setParameter(2, settlementId)
                .executeUpdate();

        em.flush();
        em.clear();

        boolean hasLinkAfter = refundSettlementLinkRepository.existsBySettlementId(settlementId);
        assertThat(hasLinkAfter).isTrue();
        System.out.println("hasLinkAfter = " + hasLinkAfter);
    }
}