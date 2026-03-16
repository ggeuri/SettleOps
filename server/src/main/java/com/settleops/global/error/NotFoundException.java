package com.settleops.global.error;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BusinessException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    @Override
    public ErrorResponse toErrorResponse() {
        return ErrorResponse.ofNotFound(getMessage());
    }
}