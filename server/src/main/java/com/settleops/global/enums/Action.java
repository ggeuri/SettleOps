package com.settleops.global.enums;

/**
 * 시스템 전역 Action 표준 ENUM (Freeze 대상)<br>
 *<br>
 * - 로그, 이벤트, 감사(Audit), 배치 결과 기록에서 공통 사용<br>
 * - 문서/코드/로그 문자열은 반드시 이 ENUM 이름과 100% 동일해야 함<br>
 * - 대소문자 및 언더스코어 포함 변경 금지<br>
 *<br>
 * B 파트 표준 오너 관리 대상
 */
public enum Action {

    // ===================== PAYMENT =====================
    PAYMENT_CREATED,        // 결제 생성
    PAYMENT_CAPTURED,       // 결제 캡처(승인 완료)
    PAYMENT_CONFIRMED,      // 결제 확정

    // ===================== BATCH =====================
    BATCH_RUN_TRIGGERED,    // 배치 실행 트리거 발생
    BATCH_RUN_SKIPPED,      // 배치 실행 스킵
    BATCH_RUN_COMPLETED,    // 배치 실행 완료

    // ===================== SETTLEMENT =====================
    SETTLEMENT_CREATED,         // 정산 생성
    SETTLEMENT_STATUS_CHANGED,  // 정산 상태 변경
    SETTLEMENT_PAY_REQUESTED,   // 지급 요청
    SETTLEMENT_PAY_APPROVED,    // 지급 승인

    // ===================== HOLD =====================
    HOLD_REQUESTED,     // 홀드 요청
    HOLD_APPROVED,      // 홀드 승인
    HOLD_RELEASED,      // 홀드 해제

    // ===================== REFUND =====================
    REFUND_REQUESTED,   // 환불 요청
    REFUND_APPROVED,    // 환불 승인
    REFUND_REJECTED     // 환불 거절
}