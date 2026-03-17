package com.settleops.domain.payment.api.dto;

/**
 * 결제 확정 여부 필터.
 *
 * <p>CONFIRMED / UNCONFIRMED 값을 사용하며,
 * null이면 확정 여부 조건을 적용하지 않는다.</p>
 */
public enum ConfirmedFilter {
    CONFIRMED,
    UNCONFIRMED
}