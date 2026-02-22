package com.settleops.domain.settlement.entity;

import com.settleops.domain.settlement.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "settlement",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_settlement_merchant_base_date", columnNames = {"merchant_id", "base_date"})
        },
        indexes = {
                @Index(name = "idx_settlement_status", columnList = "status"),
                @Index(name = "idx_settlement_base_date", columnList = "base_date"),
                @Index(name = "idx_settlement_batch", columnList = "batch_id"),
                @Index(name = "idx_settlement_merchant_base_date", columnList = "merchant_id, base_date"),
                @Index(name = "idx_settlement_status_base_date", columnList = "status, base_date")
        }
)
public class Settlement {
    @Id
    @Column(name = "settlement_id", nullable = false, columnDefinition = "CHAR(36)")
    private String settlementId;

    @Column(name = "settlement_no", nullable = false, length = 50)
    private String settlementNo;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "merchant_id", nullable = false, length = 32)
    private String merchantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SettlementStatus status;

    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;

    @Column(name = "gross", nullable = false)
    private long gross;

    @Column(name = "fee", nullable = false)
    private long fee;

    @Column(name = "vat", nullable = false)
    private long vat;

    @Column(name = "net", nullable = false)
    private long net;

    @Column(name = "paid_requested_by", length = 32)
    private String paidRequestedBy;

    @Column(name = "paid_requested_at")
    private LocalDateTime paidRequestedAt;

    @Column(name = "paid_approved_by", length = 32)
    private String paidApprovedBy;

    @Column(name = "paid_approved_at")
    private LocalDateTime paidApprovedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Settlement(){
    }

    public static Settlement createReady(
            String settlementId,
            String settlementNo,
            Long batchId,
            String merchantId,
            LocalDate baseDate,
            long gross,
            long fee,
            long vat,
            long net
    ) {
        Settlement s = new Settlement();
        s.settlementId = settlementId;
        s.settlementNo = settlementNo;
        s.batchId = batchId;
        s.merchantId = merchantId;
        s.baseDate = baseDate;
        s.gross = gross;
        s.fee = fee;
        s.vat = vat;
        s.net = net;

        s.status = SettlementStatus.READY;
        return s;
    }

    public boolean isReady() {
        return this.status == SettlementStatus.READY;
    }

    public boolean isPayRequested() {
        return this.status == SettlementStatus.PAY_REQUESTED;
    }

    public boolean isPaid() {
        return this.status == SettlementStatus.PAID;
    }

    public boolean isHoldActive() {
        return this.status == SettlementStatus.HOLD_ACTIVE;
    }

    public boolean canRequestPaid() {
        return isReady();
    }

    public boolean canApprovePaid() {
        return isPayRequested();
    }

    // 4-Eyes 검증: 승인자 ID는 필수이며, 요청자(paidRequestedBy)와 동일하면 위반
    public boolean violatesFourEyes(String approverId) {
        if (approverId == null || approverId.isBlank()) {
            throw new IllegalStateException("approverId must not be null/blank");
        }
        if (this.paidRequestedBy == null || this.paidRequestedBy.isBlank()) {
            throw new IllegalStateException("paidRequestedBy must not be null/blank");
        }
        return this.paidRequestedBy.equals(approverId);
    }

    public void requestPaid(String requesterId, LocalDateTime at) {
        if (requesterId == null || requesterId.isBlank()) {
            throw new IllegalStateException("requesterId must not be null/blank");
        }
        if (!canRequestPaid()) {
            throw new IllegalStateException("cannot request paid in the current state");
        }
        this.status = SettlementStatus.PAY_REQUESTED;
        this.paidRequestedBy = requesterId;
        this.paidRequestedAt = (at != null ? at : LocalDateTime.now());
    }

    public void approvePaid(String approverId, LocalDateTime at) {
        // 1) 상태 검증: PAY_REQUESTED 상태가 아니면 승인 불가
        if (!canApprovePaid()) {
            throw new IllegalStateException("cannot approve paid in the current state");
        }

        // 2) 4-Eyes 검증: approverId가 null/blank면 예외, 요청자와 승인자가 같으면 위반
        if (violatesFourEyes(approverId)) {
            throw new IllegalStateException("four-eyes violation: requester and approver must be different");
        }
        this.status = SettlementStatus.PAID;
        this.paidApprovedBy = approverId;
        this.paidApprovedAt = (at != null ? at : LocalDateTime.now());
    }
}
