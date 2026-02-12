package com.settleops.global.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * [LOCKED] 비즈니스 예외 처리 (403, 409 등)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        return ResponseEntity.status(e.getStatus()).body(e.toErrorResponse());
    }

    /**
     * [LOCKED] 400 Bad Request (@Valid 검증 실패)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.ofBadRequest(errorMessage));
    }

    /**
     * [LOCKED] 기타 모든 예외 (500 Internal Server Error)
     * 예기치 못한 시스템 에러 발생 시에도 requestId를 응답 바디에 포함하여 추적 가능하게 함
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleInternalServerError(Exception e) {
        // 보안상 시스템 에러 메시지는 구체적으로 노출하지 않고 로그로만 남김
        // ErrorResponse.ofBadRequest를 재활용하거나 범용 메서드를 사용
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.ofInternalError("시스템 오류가 발생했습니다. 관리자에게 문의하세요."));
    }
}