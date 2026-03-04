package com.settleops.domain.refund.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(
        name = "refund_settlement_link",
        indexes = {
                @Index(name = "idx_rsl_settlement_id", columnList = "settlement_id")
        }
)
public class RefundSettlementLink {

    @Id
    @Column(name = "refund_id", nullable = false, columnDefinition = "CHAR(36)")
    private String refundId;

    @Column(name = "settlement_id", nullable = false, columnDefinition = "CHAR(36)")
    private String settlementId;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime createdAt;

    public static RefundSettlementLink of(String refundId, String settlementId, LocalDateTime createdAt) {
        return RefundSettlementLink.builder()
                .refundId(refundId)
                .settlementId(settlementId)
                .createdAt(createdAt)
                .build();
    }
}