package com.settleops.global.error;

import org.springframework.http.HttpStatus;

/**
 * Forbidden 전용 예외
 * */
public class ForbiddenException extends BusinessException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }

    @Override
    public ErrorResponse toErrorResponse() {
        // 기획서 403 정책: reason 사용 금지 (null 고정)
        return ErrorResponse.ofForbidden(getMessage());
    }
}
