package com.settleops.global.error;

import org.springframework.http.HttpStatus;

public class BadRequestException extends BusinessException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

    @Override
    public ErrorResponse toErrorResponse() {
        return ErrorResponse.ofBadRequest(getMessage());
    }
}