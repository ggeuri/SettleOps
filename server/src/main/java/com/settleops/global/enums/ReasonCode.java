package com.settleops.global.enums;

/**
 * MVP 고정 ReasonCode 목록 (LOCKED)
 * - 409 Conflict 응답의 reason 코드로 사용
 * - 문서 / 로그 / 예외 응답과 동일 문자열 유지 (SoT)
 * - BUYER_MISMATCH는 403 전용이므로 본 ENUM에 포함하지 않음
 */
public enum ReasonCode {

    /** 주문이 이미 결제 완료된 상태 */
    ORDER_ALREADY_PAID,

    /** 결제가 capture 되지 않은 상태 */
    PAYMENT_NOT_CAPTURED,

    /** 결제가 이미 capture 된 상태 */
    PAYMENT_ALREADY_CAPTURED,

    /** 이미 환불이 존재하는 경우 */
    REFUND_ALREADY_EXISTS,

    /** Hold가 활성 상태 */
    HOLD_ACTIVE,

    /** Hold가 비활성 상태 */
    HOLD_NOT_ACTIVE,

    /** 이미 Hold가 존재하는 경우 */
    HOLD_ALREADY_EXISTS,

    /** 환불 가능 금액이 부족함 */
    INSUFFICIENT_REFUNDABLE,

    /** 배치 실행 실패 */
    BATCH_FAILED,

    /** 정산이 아직 지급 가능 상태가 아님 */
    SETTLEMENT_NOT_READY,

    /** 지급 요청이 선행되어야 함 */
    PAY_REQUESTED_REQUIRED,

    /** 동일 승인자는 승인 불가 (이중 승인 방지) */
    SAME_APPROVER_NOT_ALLOWED,

    /** 이미 지급 완료된 상태 */
    PAID_ALREADY,

    /** 현재 처리 진행 중 */
    IN_PROGRESS,

    /** 환불 차감 정산이 아직 반영 대기 상태 */
    REFUND_ADJUSTMENT_PENDING
}