package com.settleops.domain.hold.domain;

import com.settleops.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "hold",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_hold_settlement_id", columnNames = "settlement_id")
        },
        indexes = {
                @Index(name = "idx_hold_status", columnList = "status"),
                @Index(name = "idx_hold_created_at", columnList = "created_at"),
                @Index(name = "idx_hold_updated_at", columnList = "updated_at")
        })
public class Hold extends BaseEntity {

    @Id
    @Column(name="hold_id", columnDefinition = "char(36)", nullable = false, updatable = false)
    private String holdId;

    @Column(name="settlement_id", columnDefinition = "char(36)", nullable = false, updatable = false)
    private String settlementId;

    @Enumerated(EnumType.STRING)
    @Column(name="status", columnDefinition = "VARCHAR(32)", nullable = false)
    private HoldStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name="requested_reason_code",columnDefinition = "VARCHAR(64)", nullable = false)
    private HoldReasonCode requestedReasonCode;

    @Column(name = "requested_comment", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String requestedComment;

    @Column(name="created_by", columnDefinition = "VARCHAR(32)", nullable = false , updatable = false)
    private String createdBy;

    public static Hold requested(String settlementId, HoldReasonCode reasonCode, String comment, String createdBy){
        if (comment == null || comment.isBlank()) throw new IllegalArgumentException("comment is required");
        if (createdBy == null || createdBy.isBlank()) throw new IllegalArgumentException("createdBy is required");
        if (settlementId == null || settlementId.isBlank()) throw new IllegalArgumentException("settlementId is required");
        if (reasonCode == null) throw new IllegalArgumentException("reasonCode is required");

        Hold hold = new Hold();
        hold.holdId = UUID.randomUUID().toString();
        hold.settlementId = settlementId;
        hold.status = HoldStatus.HOLD_REQUESTED;
        hold.requestedReasonCode = reasonCode;
        hold.requestedComment = comment;
        hold.createdBy = createdBy;

        return hold;
    }

    // 상태 전이 메서드(approve/release에서 사용)
    public void approve() {
        if (this.status == HoldStatus.HOLD_REQUESTED) {
            this.status = HoldStatus.HOLD_ACTIVE;
        }
    }
    public void release() {
        if (this.status == HoldStatus.HOLD_ACTIVE) {
            this.status = HoldStatus.RELEASED;
        }

    }

    }

/*
 * =========================================================
 * [Schema Reference] hold (DESCRIBE style)
 * - Source: Flyway DDL (V1__DDL)  // CREATE TABLE hold ...
 * =========================================================

 * +----------------------+--------------+------+-----+---------+-------+
 * | Field                | Type         | Null | Key | Default | Extra |
 * +----------------------+--------------+------+-----+---------+-------+
 * | hold_id              | char(36)     | NO   | PRI | NULL    |       |
 * | settlement_id        | char(36)     | NO   | UNI | NULL    |       |
 * | status               | varchar(32)  | NO   | MUL | NULL    |       |
 * | requested_reason_code| varchar(64)  | NO   |     | NULL    |       |
 * | requested_comment    | text         | NO   |     | NULL    |       |
 * | created_by           | varchar(32)  | NO   |     | NULL    |       |
 * | created_at           | datetime(6)  | NO   | MUL | NULL    |       |
 * | updated_at           | datetime(6)  | NO   | MUL | NULL    |       |
 * +----------------------+--------------+------+-----+---------+-------+

 * [Keys]
 * - PRIMARY KEY (hold_id)
 * - UNIQUE KEY uk_hold_settlement_id (settlement_id)
 * - KEY idx_hold_status (status)
 * - KEY idx_hold_created_at (created_at)
 * - KEY idx_hold_updated_at (updated_at)

 * NOTE: created_at/updated_at은 Hold 엔티티에 직접 선언되어 있지 않고,
 * BaseEntity(@MappedSuperclass)에서 상속 매핑된다.
 */