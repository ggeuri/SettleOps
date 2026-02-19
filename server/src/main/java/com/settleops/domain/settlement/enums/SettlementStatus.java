package com.settleops.domain.settlement.enums;

public enum SettlementStatus {
    PENDING,        // 배치 생성 직후 (검증 전)
    READY,          // 정합성 검증 완료 (지급 대기)
    HOLD_ACTIVE,    // 운영자 Hold 걸림 (지급 차단)
    PAY_REQUESTED,  // 지급 요청됨 (1차 승인)
    PAID            // 지급 완료 (최종 승인)
}
