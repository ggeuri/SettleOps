package com.settleops.domain.settlement.entity;

import com.settleops.domain.settlement.enums.SettlementLineType;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

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
public class SettlementLine {
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
    private SettlementLineType lineType; // payment / refund

    @Column(name = "amount", nullable = false)
    private long amount; //항상 양수

    @CreationTimestamp
    @Column(name="created_at", nullable=false, updatable=false)
    private LocalDateTime createdAt;

    protected SettlementLine(){
    }

    public static SettlementLine of(
            String settlementId,
            String paymentId,
            SettlementLineType lineType,
            long amount
    ){
        if(amount <= 0) throw new IllegalArgumentException("amount must be positive");
        if (lineType == null) throw new IllegalArgumentException("lineType must not be null");
        SettlementLine l = new SettlementLine();
        l.settlementId = settlementId;
        l.paymentId = paymentId;
        l.lineType = lineType;
        l.amount = amount;
        return l;
    }

    /** 정합성 계산용: DDL상 amount는 양수이고, 부호는 line_type으로만 해석한다. */
    public long signedAmount() {
        return lineType.applySign(amount);
    }
}
