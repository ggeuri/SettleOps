package com.settleops.domain.refund.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "refund")
public class Refund {

    @Id
    @Column(name = "refund_id",
            nullable = false,
            columnDefinition = "CHAR(36)")
    private String refundId;

    @Column(name = "payment_id",
            nullable = false,
            columnDefinition = "CHAR(36)")
    private String paymentId;

    @Column(name = "merchant_id",
            nullable = false,
            length = 32)
    private String merchantId;

    @Column(name = "buyer_id",
            nullable = false,
            length = 32)
    private String buyerId;

    @Column(name = "amount",
            nullable = false)
    private long amount;

    @Column(name = "currency",
            nullable = false,
            columnDefinition = "CHAR(3)")
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status",
            nullable = false,
            length = 32)
    private RefundStatus status;

    @Column(name = "reason_text",
            nullable = false,
            length = 255)
    private String reasonText;

    @Column(name = "requested_at",
            nullable = false,
            columnDefinition = "DATETIME(6)")
    private LocalDateTime requestedAt;

    @Column(name = "decided_at",
            columnDefinition = "DATETIME(6)")
    private LocalDateTime decidedAt;

    @Column(name = "created_at",
            nullable = false,
            columnDefinition = "DATETIME(6)")
    private LocalDateTime createdAt;

    @Column(name = "updated_at",
            nullable = false,
            columnDefinition = "DATETIME(6)")
    private LocalDateTime updatedAt;

    public void approve(LocalDateTime decidedAt) {
        // 결정은 REQUESTED에서만
        if (this.status != RefundStatus.REQUESTED) {
            throw new IllegalStateException("Refund is not in REQUESTED status");
        }
        this.status = RefundStatus.APPROVED;
        this.decidedAt = decidedAt;
        this.updatedAt = decidedAt; // 또는 LocalDateTime.now()
    }

    public void reject(LocalDateTime decidedAt) {
        if (this.status != RefundStatus.REQUESTED) {
            throw new IllegalStateException("Refund is not in REQUESTED status");
        }
        this.status = RefundStatus.REJECTED;
        this.decidedAt = decidedAt;
        this.updatedAt = decidedAt; // 또는 LocalDateTime.now()
    }
}