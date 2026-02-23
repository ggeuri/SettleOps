package com.settleops.domain.settlement.entity;

import com.settleops.domain.settlement.enums.SettlementBatchResult;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

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
public class SettlementBatch {

    private static final int FAIL_REASON_MAX_LEN = 2000; // TEXT지만 화면/로그용 요약이라 과도한 폭주 방지
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
    @Column( name = "result", nullable = false, length = 20)
    private SettlementBatchResult result; // ok, fail (skip은 audit_log에)

    @Column(name = "request_id", nullable = false, columnDefinition = "CHAR(36)")
    private String requestId;

    /**
     * DDL: TEXT NULL, '[운영] FAIL 시 에러 메시지 원본(옵션)'
     * - FAIL 기록이 절대 롤백되면 안 되므로(운영 재현/SoT 방향),
     *   FAIL인데 비어있으면 기본값으로 보정한다.
     */
    @Column(name = "fail_reason", columnDefinition = "TEXT")
    private String failReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    protected SettlementBatch(){
    }

    public static SettlementBatch completed(
            LocalDate batchKey,
            String runId,
            String triggeredBy,
            SettlementBatchResult result,
            String requestId,
            String failReason,
            LocalDateTime finishedAt
    ){
        if (result == null) throw new IllegalArgumentException("result must not be null");
        if (batchKey == null) throw new IllegalArgumentException("batchKey must not be null");
        if (runId == null || runId.isBlank()) throw new IllegalArgumentException("runId must not be blank");
        if (triggeredBy == null || triggeredBy.isBlank()) throw new IllegalArgumentException("triggeredBy must not be blank");
        if (requestId == null || requestId.isBlank()) throw new IllegalArgumentException("requestId must not be blank");

        String normalizedFailReason = normalizeFailReason(result, failReason);

        SettlementBatch b = new SettlementBatch();
        b.batchKey = batchKey;
        b.runId = runId;
        b.triggeredBy = triggeredBy;
        b.result = result;
        b.requestId = requestId;
        b.failReason = normalizedFailReason;
        b.finishedAt = finishedAt;
        return b;
    }

    private static String normalizeFailReason(SettlementBatchResult result, String failReason) {
        if (result == SettlementBatchResult.OK) {
            // OK에서는 failReason을 남기지 않음(있어도 저장 실패시키지 않고 null로 정규화)
            return null;
        }

        // FAIL: 없으면 기본값으로 보정
        String v = (failReason == null) ? "" : failReason.trim();
        if (v.isEmpty()) v = DEFAULT_FAIL_REASON;

        // 폭주 방지(너무 긴 stacktrace/메시지 통째로 넣는 실수 방지)
        if (v.length() > FAIL_REASON_MAX_LEN) {
            v = v.substring(0, FAIL_REASON_MAX_LEN);
        }
        return v;
    }
}
