package com.settleops.domain.settlement.entity;

import com.settleops.domain.settlement.enums.SettlementLineType;
import com.settleops.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Getter
@Table(
        name = "settlement_line",
        indexes = {
                @Index(name = "idx_settlement_line_payment_id", columnList = "payment_id"),
                @Index(name = "idx_settlement_line_settlement_id", columnList = "settlement_id"),
                @Index(name = "idx_settlement_line_type", columnList = "line_type")
        }
)
public class SettlementLine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_line_id", nullable = false)
    private Long settlementLineId;

    @Column(name = "settlement_id", nullable = false, columnDefinition = "CHAR(36)")
    private String settlementId;

    @Column(name = "payment_id", nullable = false, columnDefinition = "CHAR(36)")
    private String paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_type", nullable = false, length = 20)
    private SettlementLineType lineType; // PAYMENT / REFUND

    @Column(name = "amount", nullable = false)
    private long amount; // 항상 양수

    protected SettlementLine() {
    }

    public static SettlementLine of(
            String settlementId,
            String paymentId,
            SettlementLineType lineType,
            long amount
    ) {
        if (settlementId == null || settlementId.isBlank()) {
            throw new IllegalArgumentException("settlementId must not be blank");
        }
        if (paymentId == null || paymentId.isBlank()) {
            throw new IllegalArgumentException("paymentId must not be blank");
        }
        if (lineType == null) {
            throw new IllegalArgumentException("lineType must not be null");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        SettlementLine l = new SettlementLine();
        l.settlementId = settlementId;
        l.paymentId = paymentId;
        l.lineType = lineType;
        l.amount = amount;
        return l;
    }

    public long signedAmount() {
        return lineType.applySign(amount);
    }
}