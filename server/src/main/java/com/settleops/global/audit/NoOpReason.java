package com.settleops.global.audit;

public enum NoOpReason {
    // Confirm
    ALREADY_CONFIRMED,

    // Hold / Refund 공용
    ALREADY_APPROVED,
    ALREADY_RELEASED,
    ALREADY_REJECTED,

    // Paid
    ALREADY_PAID,
    ALREADY_PAY_REQUESTED,

    // Batch
    BATCH_KEY_EXISTS,

    // Payment
    ALREADY_CAPTURED
}