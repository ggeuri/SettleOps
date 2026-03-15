package com.settleops.domain.settlement.infra;

import com.settleops.domain.refund.domain.Refund;
import com.settleops.domain.refund.domain.RefundSettlementLink;
import com.settleops.domain.refund.domain.RefundStatus;
import com.settleops.domain.refund.infra.RefundRepository;
import com.settleops.domain.refund.infra.RefundSettlementLinkRepository;
import com.settleops.domain.settlement.dto.AdminSettlementHoldSummaryResponse;
import com.settleops.domain.settlement.dto.AdminSettlementLineItemResponse;
import com.settleops.domain.settlement.dto.AdminSettlementRefundSummaryResponse;
import com.settleops.domain.settlement.entity.Hold;
import com.settleops.domain.settlement.entity.Settlement;
import com.settleops.domain.settlement.entity.SettlementLine;
import com.settleops.domain.settlement.enums.HoldStatus;
import com.settleops.domain.settlement.enums.SettlementLineType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SettlementDetailQueryRepositoryImplTest {

    @Autowired
    private SettlementDetailQueryRepository settlementDetailQueryRepository;

    @Autowired
    private SettlementRepository settlementRepository;

    @Autowired
    private SettlementLineRepository settlementLineRepository;

    @Autowired
    private HoldRepository holdRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private RefundSettlementLinkRepository refundSettlementLinkRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("A4 상세 조회용 line 목록은 createdAt asc 순으로 반환된다")
    void findSettlementLines_ordersByCreatedAtAsc() {
        // given
        String settlementId = UUID.randomUUID().toString();

        Settlement settlement = createSettlement(
                settlementId,
                "merchant-1",
                LocalDate.of(2026, 3, 10),
                10000L
        );
        settlementRepository.save(settlement);

        SettlementLine laterLine = SettlementLine.of(
                settlementId,
                "payment-2",
                SettlementLineType.PAYMENT,
                3000L
        );
        SettlementLine earlierLine = SettlementLine.of(
                settlementId,
                "payment-1",
                SettlementLineType.PAYMENT,
                7000L
        );

        settlementLineRepository.saveAll(List.of(laterLine, earlierLine));
        entityManager.flush();

        updateSettlementLineCreatedAt(earlierLine.getSettlementLineId(), LocalDateTime.of(2026, 3, 11, 9, 0));
        updateSettlementLineCreatedAt(laterLine.getSettlementLineId(), LocalDateTime.of(2026, 3, 11, 10, 0));
        entityManager.clear();

        // when
        List<AdminSettlementLineItemResponse> result =
                settlementDetailQueryRepository.findSettlementLines(settlementId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).paymentId()).isEqualTo("payment-1");
        assertThat(result.get(1).paymentId()).isEqualTo("payment-2");
    }

    @Test
    @DisplayName("A4 상세 조회용 hold summary는 hold가 있으면 실제 값을 반환한다")
    void findHoldSummary_returnsActualValueWhenHoldExists() {
        // given
        String settlementId = UUID.randomUUID().toString();

        Settlement settlement = createSettlement(
                settlementId,
                "merchant-1",
                LocalDate.of(2026, 3, 10),
                10000L
        );
        settlementRepository.save(settlement);

        Hold hold = Hold.builder()
                .holdId(UUID.randomUUID().toString())
                .settlementId(settlementId)
                .status(HoldStatus.HOLD_ACTIVE)
                .requestedReasonCode("SUSPECTED_FRAUD")
                .requestedComment("운영 확인 필요")
                .createdBy("admin1")
                .build();

        holdRepository.save(hold);
        entityManager.flush();
        entityManager.clear();

        // when
        AdminSettlementHoldSummaryResponse result =
                settlementDetailQueryRepository.findHoldSummary(settlementId);

        // then
        assertThat(result.exists()).isTrue();
        assertThat(result.holdId()).isEqualTo(hold.getHoldId());
        assertThat(result.status()).isEqualTo(HoldStatus.HOLD_ACTIVE.name());
        assertThat(result.reasonCode()).isEqualTo("SUSPECTED_FRAUD");
        assertThat(result.comment()).isEqualTo("운영 확인 필요");
        assertThat(result.createdBy()).isEqualTo("admin1");
        assertThat(result.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("A4 상세 조회용 refund summary는 approved refund가 있고 link가 없으면 반영 대기 상태를 반환한다")
    void findRefundSummary_returnsPendingTrueWhenApprovedRefundExistsWithoutLink() {
        // given
        String settlementId = UUID.randomUUID().toString();

        Settlement settlement = createSettlement(
                settlementId,
                "merchant-1",
                LocalDate.of(2026, 3, 10),
                10000L
        );
        settlementRepository.save(settlement);

        SettlementLine line = SettlementLine.of(
                settlementId,
                "payment-1",
                SettlementLineType.PAYMENT,
                10000L
        );
        settlementLineRepository.save(line);

        Refund refund = createRefund(
                "refund-1",
                "payment-1",
                RefundStatus.APPROVED
        );
        refundRepository.save(refund);

        entityManager.flush();
        entityManager.clear();

        // when
        AdminSettlementRefundSummaryResponse result =
                settlementDetailQueryRepository.findRefundSummary(settlementId);

        // then
        assertThat(result.hasApprovedRefund()).isTrue();
        assertThat(result.refundAdjustmentPending()).isTrue();
    }

    @Test
    @DisplayName("A4 상세 조회용 refund summary는 approved refund가 link로 반영되었으면 반영 대기 상태가 아니다")
    void findRefundSummary_returnsPendingFalseWhenApprovedRefundLinkExists() {
        // given
        String settlementId = UUID.randomUUID().toString();

        Settlement settlement = createSettlement(
                settlementId,
                "merchant-1",
                LocalDate.of(2026, 3, 10),
                10000L
        );
        settlementRepository.save(settlement);

        SettlementLine line = SettlementLine.of(
                settlementId,
                "payment-1",
                SettlementLineType.PAYMENT,
                10000L
        );
        settlementLineRepository.save(line);

        Refund refund = createRefund(
                "refund-1",
                "payment-1",
                RefundStatus.APPROVED
        );
        refundRepository.save(refund);

        RefundSettlementLink link = RefundSettlementLink.of(
                "refund-1",
                settlementId,
                LocalDateTime.of(2026, 3, 11, 12, 0)
        );
        refundSettlementLinkRepository.save(link);

        entityManager.flush();
        entityManager.clear();

        // when
        AdminSettlementRefundSummaryResponse result =
                settlementDetailQueryRepository.findRefundSummary(settlementId);

        // then
        assertThat(result.hasApprovedRefund()).isTrue();
        assertThat(result.refundAdjustmentPending()).isFalse();
    }

    private Settlement createSettlement(
            String settlementId,
            String merchantId,
            LocalDate baseDate,
            long net
    ) {
        return Settlement.createReady(
                settlementId,
                "SET-" + baseDate.toString().replace("-", "") + "-" + merchantId,
                1L,
                merchantId,
                baseDate,
                net,
                0L,
                0L,
                net
        );
    }

    private Refund createRefund(
            String refundId,
            String paymentId,
            RefundStatus status
    ) {
        LocalDateTime now = LocalDateTime.of(2026, 3, 11, 10, 0);

        return Refund.builder()
                .refundId(refundId)
                .paymentId(paymentId)
                .merchantId("merchant-1")
                .buyerId("buyer-1")
                .amount(3000L)
                .currency("KRW")
                .status(status)
                .reasonText("단순 변심")
                .requestedAt(now.minusHours(1))
                .decidedAt(status == RefundStatus.APPROVED ? now : null)
                .build();
    }

    private void updateSettlementLineCreatedAt(Long settlementLineId, LocalDateTime createdAt) {
        jdbcTemplate.update(
                "UPDATE settlement_line SET created_at = ? WHERE settlement_line_id = ?",
                Timestamp.valueOf(createdAt),
                settlementLineId
        );
    }

    @Test
    @DisplayName("A4 상세 조회용 refund summary는 approved refund가 다른 settlement에 반영되어 있어도 pending이 아니다")
    void findRefundSummary_returnsPendingFalseWhenApprovedRefundAlreadyLinkedToAnotherSettlement() {
        // given
        String settlementId = UUID.randomUUID().toString();
        String anotherSettlementId = UUID.randomUUID().toString();

        Settlement settlement = createSettlement(
                settlementId,
                "merchant-1",
                LocalDate.of(2026, 3, 10),
                10000L
        );
        settlementRepository.save(settlement);

        Settlement anotherSettlement = createSettlement(
                anotherSettlementId,
                "merchant-1",
                LocalDate.of(2026, 3, 11),
                9000L
        );
        settlementRepository.save(anotherSettlement);

        SettlementLine line = SettlementLine.of(
                settlementId,
                "payment-1",
                SettlementLineType.PAYMENT,
                10000L
        );
        settlementLineRepository.save(line);

        Refund refund = createRefund(
                "refund-1",
                "payment-1",
                RefundStatus.APPROVED
        );
        refundRepository.save(refund);

        RefundSettlementLink link = RefundSettlementLink.of(
                "refund-1",
                anotherSettlementId,
                LocalDateTime.of(2026, 3, 11, 12, 0)
        );
        refundSettlementLinkRepository.save(link);

        entityManager.flush();
        entityManager.clear();

        // when
        AdminSettlementRefundSummaryResponse result =
                settlementDetailQueryRepository.findRefundSummary(settlementId);

        // then
        assertThat(result.hasApprovedRefund()).isTrue();
        assertThat(result.refundAdjustmentPending()).isFalse();
    }
}