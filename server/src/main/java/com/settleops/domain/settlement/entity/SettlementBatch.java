package com.settleops.domain.settlement.entity;

import com.settleops.domain.settlement.enums.SettlementBatchResult;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "settlement_batch",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_settlement_batch_key", columnNames = "batch_key")
        },
        indexes = {
                @Index(name = "idx_settlement_batch_request_id", columnList = "request_id"),
                @Index(name = "idx_settlement_batch_created_at", columnList = "created_at")
        }
)
public class SettlementBatch extends BaseEntity {

    private static final int FAIL_REASON_MAX_LEN = 2000;
    private static final String DEFAULT_FAIL_REASON = "UNEXPECTED_ERROR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "batch_key", nullable = false)
    private LocalDate batchKey;

    @Column(name = "run_id", nullable = false, columnDefinition = "CHAR(36)")
    private String runId;

    @Column(name = "triggered_by", nullable = false, length = 50)
    private String triggeredBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20)
    private SettlementBatchResult result; // OK / FAIL (SKIP은 audit_log)

    @Column(name = "request_id", nullable = false, columnDefinition = "CHAR(36)")
    private String requestId;

    @Column(name = "fail_reason", columnDefinition = "TEXT")
    private String failReason;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    protected SettlementBatch() {
    }

    public static SettlementBatch completed(
            LocalDate batchKey,
            String runId,
            String triggeredBy,
            SettlementBatchResult result,
            String requestId,
            String failReason,
            LocalDateTime finishedAt
    ) {
        if (result == null) throw new IllegalArgumentException("result must not be null");
        if (batchKey == null) throw new IllegalArgumentException("batchKey must not be null");
        if (runId == null || runId.isBlank()) throw new IllegalArgumentException("runId must not be blank");
        if (triggeredBy == null || triggeredBy.isBlank()) throw new IllegalArgumentException("triggeredBy must not be blank");
        if (requestId == null || requestId.isBlank()) throw new IllegalArgumentException("requestId must not be blank");

        SettlementBatch b = new SettlementBatch();
        b.batchKey = batchKey;
        b.runId = runId;
        b.triggeredBy = triggeredBy;
        b.result = result;
        b.requestId = requestId;
        b.failReason = normalizeFailReason(result, failReason);
        b.finishedAt = finishedAt;
        return b;
    }

    private static String normalizeFailReason(SettlementBatchResult result, String failReason) {
        if (result == SettlementBatchResult.OK) {
            return null;
        }

        String v = (failReason == null) ? "" : failReason.trim();
        if (v.isEmpty()) v = DEFAULT_FAIL_REASON;

        if (v.length() > FAIL_REASON_MAX_LEN) {
            v = v.substring(0, FAIL_REASON_MAX_LEN);
        }
        return v;
    }
}