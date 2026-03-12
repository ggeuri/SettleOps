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
        if (reasonCode == null) {
            throw new IllegalArgumentException("ReasonCode must not be null for 409 Conflict");
        }
        this.reasonCode = reasonCode;
    }

    public ReasonCode getReasonCode() {
        return reasonCode;
    }

    @Override
    public ErrorResponse toErrorResponse() {
        // 기획서 409 포맷: code는 RULE_VIOLATION, reason은 필수
        return ErrorResponse.ofRuleViolation(reasonCode, getMessage());
    }
}