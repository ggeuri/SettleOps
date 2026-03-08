package com.settleops.global.error;

import com.settleops.global.enums.ReasonCode;

/**
 * 전역 에러 응답 표준 DTO (GlobalExceptionHandler 전용)
 *
 * <p>[HTTP 정책]</p>
 * <p>409 Conflict → ReasonCode 필수 (reason != null)</p>
 * <p>403 Forbidden → reason 사용 금지 (null 고정, BUYER_MISMATCH는 message로 처리)</p>
 * <p>400 Bad Request → Validation 전용</p>
 *
 * <p>[규칙]</p>
 * <p>- code는 정책상 고정 문자열 사용 (예: RULE_VIOLATION)</p>
 * <p>- reason은 반드시 ReasonCode.name() 사용</p>
 * <p>- 문자열 하드코딩 금지</p>
 */

public record ErrorResponse(
        String code,          // RULE_VIOLATION 등
        String reason,        // ReasonCode.name() (409 전용)
        String message        // 사용자 메시지
) {

    private static final String UNAUTHORIZED = "UNAUTHORIZED";
    private static final String RULE_VIOLATION = "RULE_VIOLATION";
    private static final String FORBIDDEN = "FORBIDDEN";
    private static final String BAD_REQUEST = "BAD_REQUEST";
    private static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";

    // 401 UnauthorizedException용
    public static ErrorResponse ofUnauthorized(String message) {
        return new ErrorResponse(UNAUTHORIZED, null, message);
    }
    // 기획서 0-2 정책: 409 Conflict 전용 정적 팩토리 메서드
    public static ErrorResponse ofRuleViolation(ReasonCode reason, String message) {
        return new ErrorResponse(RULE_VIOLATION, reason.name(), message);
    }

    // 403 Forbidden용 (reason은 무조건 null)
    public static ErrorResponse ofForbidden(String message) {
        return new ErrorResponse(FORBIDDEN, null, message);
    }

    // 400 Bad Request 또는 기타 4xx용
    public static ErrorResponse ofBadRequest(String message) {
        return new ErrorResponse(BAD_REQUEST, null, message);
    }

    // 500 Internal Server Error용
    public static ErrorResponse ofInternalError(String message) {
        return new ErrorResponse(INTERNAL_SERVER_ERROR, null, message);
    }
}
