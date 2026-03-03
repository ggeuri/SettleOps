package com.settleops.domain.refund.domain;

import com.settleops.global.audit.ActorType;

import java.time.LocalDateTime;

public class RefundEventFactory {
    private RefundEventFactory() {}

    public static RefundEvent requested(
            String refundId,
            String requestId,
            ActorType actorType,
            String actorId,
            LocalDateTime occurredAtOrNull
    ) {
        return RefundEvent.builder()
                .refundId(refundId)
                .eventType(RefundEventType.REFUND_REQUESTED)
                .statusBefore(null) // LOCKED: REQUESTED만 before=null 허용
                .statusAfter(RefundStatus.REQUESTED)
                .requestId(requestId)
                .actorType(actorType)
                .actorId(actorId)
                .occurredAt(occurredAtOrNull) // null이면 @PrePersist가 채움
                .build();
    }

    public static RefundEvent approved(
            String refundId,
            RefundStatus beforeStatus,
            String requestId,
            ActorType actorType,
            String actorId,
            LocalDateTime occurredAtOrNull
    ) {
        requireBeforeRequested(beforeStatus, "APPROVED");
        return RefundEvent.builder()
                .refundId(refundId)
                .eventType(RefundEventType.REFUND_APPROVED)
                .statusBefore(beforeStatus) // LOCKED: REQUESTED 기대
                .statusAfter(RefundStatus.APPROVED)
                .requestId(requestId)
                .actorType(actorType)
                .actorId(actorId)
                .occurredAt(occurredAtOrNull)
                .build();
    }

    public static RefundEvent rejected(
            String refundId,
            RefundStatus beforeStatus,
            String requestId,
            ActorType actorType,
            String actorId,
            LocalDateTime occurredAtOrNull
    ) {
        requireBeforeRequested(beforeStatus, "REJECTED");
        return RefundEvent.builder()
                .refundId(refundId)
                .eventType(RefundEventType.REFUND_REJECTED)
                .statusBefore(beforeStatus) // LOCKED: REQUESTED 기대
                .statusAfter(RefundStatus.REJECTED)
                .requestId(requestId)
                .actorType(actorType)
                .actorId(actorId)
                .occurredAt(occurredAtOrNull)
                .build();
    }

    private static void requireBeforeRequested(RefundStatus before, String eventName) {
        if (before != RefundStatus.REQUESTED) {
            throw new IllegalStateException("statusBefore must be REQUESTED for " + eventName);
        }
    }
}
