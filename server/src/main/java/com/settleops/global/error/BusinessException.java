package com.settleops.global.error;

import com.settleops.global.enums.ReasonCode;
import org.springframework.http.HttpStatus;

/** 최상위 비즈니스 추상 클래스 */
public abstract class BusinessException extends RuntimeException{

    private final HttpStatus status;

    protected BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    // 에러 응답 변환 추상 메서드 (자식 구현)
    public abstract ErrorResponse toErrorResponse();

}
