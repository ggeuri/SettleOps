package com.settleops.global.error;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }

    @Override
    public ErrorResponse toErrorResponse() {
        return ErrorResponse.ofUnauthorized(getMessage());
    }
}