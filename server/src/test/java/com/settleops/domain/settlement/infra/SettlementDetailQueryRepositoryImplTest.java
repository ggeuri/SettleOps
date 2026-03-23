package com.settleops.domain.settlement.infra;

import com.settleops.domain.hold.domain.Hold;
import com.settleops.domain.hold.domain.HoldReasonCode;
import com.settleops.domain.hold.domain.HoldStatus;
import com.settleops.domain.hold.infra.HoldRepository;
import com.settleops.domain.refund.domain.Refund;
import com.settleops.domain.refund.domain.RefundStatus;
import com.settleops.domain.refund.infra.RefundRepository;
import com.settleops.domain.settlement.dto.AdminSettlementHoldSummaryResponse;
import com.settleops.domain.settlement.dto.AdminSettlementLineItemResponse;
import com.settleops.domain.settlement.entity.Settlement;
import com.settleops.domain.settlement.entity.SettlementLine;
import com.settleops.domain.settlement.enums.SettlementLineType;
import com.settleops.global.audit.ActorType;
import com.settleops.global.audit.AuditLog;
import com.settleops.global.audit.EntityType;
import com.settleops.global.enums.Action;
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
@ActiveProfiles("test-db")
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
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("A4 상세 조회용 line 목록은 createdAt asc 순으로 반환된다")
    void findSettlementLines_ordersByCreatedAtAsc() {
        String settlementId = UUID.randomUUID().toString();
        String paymentId1 = UUID.randomUUID().toString();
        String paymentId2 = UUID.randomUUID().toString();

        Settlement settlement = createSettlement(
                settlementId,
                "merchant-1",
                LocalDate.of(2026, 3, 10),
                10000L
        );
        settlementRepository.save(settlement);

        SettlementLine laterLine = SettlementLine.of(
                settlementId,
                paymentId2,
                SettlementLineType.PAYMENT,
                3000L
        );
        SettlementLine earlierLine = SettlementLine.of(
                settlementId,
                paymentId1,
                SettlementLineType.PAYMENT,
                7000L
        );

        settlementLineRepository.saveAll(List.of(laterLine, earlierLine));
        entityManager.flush();

        updateSettlementLineCreatedAt(earlierLine.getSettlementLineId(), LocalDateTime.of(2026, 3, 11, 9, 0));
        updateSettlementLineCreatedAt(laterLine.getSettlementLineId(), LocalDateTime.of(2026, 3, 11, 10, 0));
        entityManager.clear();

        List<AdminSettlementLineItemResponse> result =
                settlementDetailQueryRepository.findSettlementLines(settlementId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).paymentId()).isEqualTo(paymentId1);
        assertThat(result.get(1).paymentId()).isEqualTo(paymentId2);
    }

    @Test
    @DisplayName("A4 상세 조회용 hold summary는 hold가 있으면 실제 값을 반환한다")
    void findHoldSummary_returnsActualValueWhenHoldExists() {
        String settlementId = UUID.randomUUID().toString();

        Settlement settlement = createSettlement(
                settlementId,
                "merchant-1",
                LocalDate.of(2026, 3, 10),
                10000L
        );
        settlementRepository.save(settlement);

        Hold hold = Hold.requested(
                settlementId,
                HoldReasonCode.RISK_SUSPECTED,
                "운영 확인 필요",
                "admin1"
        );
        hold.approve();

        holdRepository.save(hold);
        entityManager.flush();
        entityManager.clear();

        AdminSettlementHoldSummaryResponse result =
                settlementDetailQueryRepository.findHoldSummary(settlementId);

        assertThat(result.exists()).isTrue();
        assertThat(result.holdId()).isEqualTo(hold.getHoldId());
        assertThat(result.status()).isEqualTo(HoldStatus.HOLD_ACTIVE.name());
        assertThat(result.reasonCode()).isEqualTo(HoldReasonCode.RISK_SUSPECTED.name());
        assertThat(result.comment()).isEqualTo("운영 확인 필요");
        assertThat(result.createdBy()).isEqualTo("admin1");
        assertThat(result.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("A4 상세 조회용 refund 조회는 approved refund가 있으면 true를 반환한다")
    void hasApprovedRefund_returnsTrueWhenApprovedRefundExists() {
        String settlementId = UUID.randomUUID().toString();
        String paymentId = UUID.randomUUID().toString();
        String refundId = UUID.randomUUID().toString();

        Settlement settlement = createSettlement(
                settlementId,
                "merchant-1",
                LocalDate.of(2026, 3, 10),
                10000L
        );
        settlementRepository.save(settlement);

        SettlementLine line = SettlementLine.of(
                settlementId,
                paymentId,
                SettlementLineType.PAYMENT,
                10000L
        );
        settlementLineRepository.save(line);

        Refund refund = createRefund(
                refundId,
                paymentId,
                RefundStatus.APPROVED
        );
        refundRepository.save(refund);

        entityManager.flush();
        entityManager.clear();

        boolean result = settlementDetailQueryRepository.hasApprovedRefund(settlementId);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("A4 상세 조회용 refund 조회는 approved refund가 없으면 false를 반환한다")
    void hasApprovedRefund_returnsFalseWhenApprovedRefundDoesNotExist() {
        String settlementId = UUID.randomUUID().toString();
        String paymentId = UUID.randomUUID().toString();

        Settlement settlement = createSettlement(
                settlementId,
                "merchant-1",
                LocalDate.of(2026, 3, 10),
                10000L
        );
        settlementRepository.save(settlement);

        SettlementLine line = SettlementLine.of(
                settlementId,
                paymentId,
                SettlementLineType.PAYMENT,
                10000L
        );
        settlementLineRepository.save(line);

        entityManager.flush();
        entityManager.clear();

        boolean result = settlementDetailQueryRepository.hasApprovedRefund(settlementId);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("A4 Trace entry 조회는 최신 no-op이 아닌 settlement requestId를 반환한다")
    void findLatestNonNoOpSettlementRequestId_returnsLatestNonNoOpRequestId() {
        String settlementId = UUID.randomUUID().toString();

        Settlement settlement = createSettlement(
                settlementId,
                "merchant-1",
                LocalDate.of(2026, 3, 10),
                10000L
        );
        settlementRepository.save(settlement);
        entityManager.flush();

        AuditLog olderNonNoOp = createAuditLog(
                "request-older",
                settlementId,
                LocalDateTime.of(2026, 3, 11, 9, 0),
                "{\"noOp\":false}"
        );
        entityManager.persist(olderNonNoOp);

        AuditLog latestNonNoOp = createAuditLog(
                "request-latest-non-noop",
                settlementId,
                LocalDateTime.of(2026, 3, 11, 10, 0),
                "{\"noOp\":false}"
        );
        entityManager.persist(latestNonNoOp);

        AuditLog latestNoOp = createAuditLog(
                "request-latest-noop",
                settlementId,
                LocalDateTime.of(2026, 3, 11, 11, 0),
                "{\"noOp\":true,\"noOpReason\":\"ALREADY_PAY_REQUESTED\"}"
        );
        entityManager.persist(latestNoOp);

        entityManager.flush();
        entityManager.clear();

        String result =
                settlementDetailQueryRepository.findLatestNonNoOpSettlementRequestId(settlementId);

        assertThat(result).isEqualTo("request-latest-non-noop");
    }

    @Test
    @DisplayName("A4 Trace entry 조회는 no-op이 아닌 settlement audit가 없으면 null을 반환한다")
    void findLatestNonNoOpSettlementRequestId_returnsNullWhenOnlyNoOpExists() {
        String settlementId = UUID.randomUUID().toString();

        Settlement settlement = createSettlement(
                settlementId,
                "merchant-1",
                LocalDate.of(2026, 3, 10),
                10000L
        );
        settlementRepository.save(settlement);
        entityManager.flush();

        AuditLog onlyNoOp = createAuditLog(
                "request-only-noop",
                settlementId,
                LocalDateTime.of(2026, 3, 11, 11, 0),
                "{\"noOp\":true,\"noOpReason\":\"ALREADY_PAY_REQUESTED\"}"
        );
        entityManager.persist(onlyNoOp);

        entityManager.flush();
        entityManager.clear();

        String result =
                settlementDetailQueryRepository.findLatestNonNoOpSettlementRequestId(settlementId);

        assertThat(result).isNull();
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

    private AuditLog createAuditLog(
            String requestId,
            String settlementId,
            LocalDateTime occurredAt,
            String metaJson
    ) {
        return AuditLog.builder()
                .requestId(requestId)
                .occurredAt(occurredAt)
                .actorType(ActorType.ADMIN)
                .actorId("admin1")
                .action(Action.SETTLEMENT_PAY_REQUESTED)
                .entityType(EntityType.SETTLEMENT)
                .entityId(settlementId)
                .statusBefore("READY")
                .statusAfter("PAY_REQUESTED")
                .merchantId("merchant-1")
                .metaJson(metaJson)
                .build();
    }

    private void updateSettlementLineCreatedAt(Long settlementLineId, LocalDateTime createdAt) {
        jdbcTemplate.update(
                "UPDATE settlement_line SET created_at = ? WHERE settlement_line_id = ?",
                Timestamp.valueOf(createdAt),
                settlementLineId
        );
    }
}