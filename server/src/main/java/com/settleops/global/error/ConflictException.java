package com.settleops.global.error;

import com.settleops.global.enums.ReasonCode;
import org.springframework.http.HttpStatus;

/**
 * 409 Conflict 전용 예외 <br>
 * (ReasonCode 강제)
 * */
public class ConflictException extends BusinessException {
    private final ReasonCode reasonCode;

    public ConflictException(ReasonCode reasonCode, String message) {
        super(HttpStatus.CONFLICT, message);
        this.reasonCode = reasonCode;
    }

    @Override
    public ErrorResponse toErrorResponse() {
        // 기획서 409 포맷: code는 RULE_VIOLATION, reason은 필수
        return ErrorResponse.ofRuleViolation(reasonCode, getMessage());
    }
}