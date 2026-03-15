package com.settleops.domain.settlement.entity;

import com.settleops.domain.settlement.enums.HoldStatus;
import com.settleops.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "hold")
public class Hold extends BaseEntity {

    @Id
    @Column(name = "hold_id", nullable = false, columnDefinition = "CHAR(36)")
    private String holdId;

    @Column(name = "settlement_id", nullable = false, columnDefinition = "CHAR(36)")
    private String settlementId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private HoldStatus status;

    @Column(name = "requested_reason_code", nullable = false, length = 64)
    private String requestedReasonCode;

    @Column(name = "requested_comment", nullable = false, length = 4000)
    private String requestedComment;

    @Column(name = "created_by", nullable = false, length = 32)
    private String createdBy;
}
