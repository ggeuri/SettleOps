package com.settleops.global.error;

import com.settleops.global.audit.AuditLogCommand;
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.audit.AuditMetaFactory;
import com.settleops.global.enums.ReasonCode;
import com.settleops.global.logging.RequestIdKeys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Comparator;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final AuditLogger auditLogger;

    /**
     * [LOCKED] 비즈니스 예외 처리 (403, 409 등)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e, HttpServletRequest request) {
        tryLog409Failure(e, request);
        return ResponseEntity.status(e.getStatus()).body(e.toErrorResponse());
    }

    /**
     * <p><b>[POLICY] 400 Bad Request (@Valid 검증 실패 처리 규칙)</b></p>
     *
     * <p><b>■ 목적</b><br>
     * - Validation 실패 시 일관된 단일 메시지 응답을 제공하여<br>
     *   테스트 안정성과 응답 예측 가능성을 확보한다.</p>
     *
     * <p><b>■ 메시지 선택 규칙</b><br>
     * 1) fieldErrors를 우선 대상으로 한다.<br>
     * 2) 필드명 기준 오름차순으로 정렬한다. (항상 동일한 순서 보장)<br>
     * 3) 정렬 후 첫 번째 메시지 1건만 반환한다. (단일 메시지 정책)<br>
     * 4) fieldError가 없을 경우 globalError에서 첫 번째 메시지를 반환한다.<br>
     * 5) 모든 에러가 비어있을 경우 기본 문구("요청 값이 올바르지 않습니다.")를 반환한다.</p>
     *
     * <p>※ 본 규칙은 현재 MVP 단계의 운영 정책이며,<br>
     * 향후 UX 요구사항 또는 기획서 변경에 따라 다중 오류 반환 구조로 확장될 수 있다.</p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        // String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        // 1. BindingResult 추출
        var bindingResult = e.getBindingResult();

        // 첫 번째 메시지 선택
        String errorMessage = bindingResult.getFieldErrors().stream()

                //      필드 별 default 메세지
                //      @NotNull(message = "결제 금액은 필수 입력 항목입니다.")
                //      @Min(value = 100, message = "결제 금액은 100원 이상이어야 합니다.")
                //      Long amount,
                //
                //      @NotBlank(message = "통화 코드는 필수 입력 항목입니다.")
                //      String currency,
                //
                //      @NotBlank(message = "가맹점 ID는 필수 입력 항목입니다.")
                //      String merchantId

                // 2. 필드명 기준 정렬 (항상 동일한 순서 보장 → 테스트 안정성 확보)
                .sorted(Comparator.comparing(fe -> fe.getField()))

                // 3. 사용자에게 노출할 실제 검증 메시지 추출
                .map(fe -> fe.getDefaultMessage())

                // 4. 첫 번째 에러 메시지만 선택 (단일 메시지 정책)
                .findFirst()

                // 5. fieldError가 없는 경우 → globalError 처리
                .orElseGet(() ->
                        bindingResult.getGlobalErrors().stream()

                                // globalError 메시지 추출
                                .map(ge -> ge.getDefaultMessage())

                                // 첫 번째 메시지 선택
                                .findFirst()

                                // 6. 모든 에러가 비어있을 경우 기본 문구 반환
                                .orElse("요청 값이 올바르지 않습니다.")
                );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.ofBadRequest(errorMessage));
    }

    /**
     * [POLICY] 필수 RequestParam 누락 처리
     * - Spring binding 단계에서 발생하는 누락 오류는 400 Bad Request로 변환한다.
     * - 메시지는 단일 정책으로 "<parameterName>는 필수입니다." 형태를 사용한다.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
            MissingServletRequestParameterException e
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.ofBadRequest(e.getParameterName() + "는 필수입니다."));
    }

    /**
     * [INTERNAL] IllegalArgumentException
     * (개발자 계약 위반/방어코드) → 500 고정 응답
     * - 응답 메시지는 내부 정책상 고정
     * - 상세 원인은 로그로만 남김
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.error("IllegalArgumentException occurred: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.ofInternalError("시스템 오류가 발생했습니다. 관리자에게 문의하세요."));
    }

    /**[LOCKED] 기타 모든 예외 (500 Internal Server Error)*/
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleInternalServerError(Exception e) {
        // 보안상 시스템 에러 메시지는 구체적으로 노출하지 않고 로그로만 남김
        // ErrorResponse.ofBadRequest를 재활용하거나 범용 메서드를 사용
        log.error("Unhandled exception occurred", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.ofInternalError("시스템 오류가 발생했습니다. 관리자에게 문의하세요."));
    }

    /**
     * [POLICY] Request 파라미터 타입 변환 실패 처리
     * - Spring binding 단계에서 enum, 숫자, 날짜 등의 타입 변환에 실패한 경우
     *   400 Bad Request로 변환한다.
     * - 예: 잘못된 enum 값(status=INVALID), 숫자 파라미터 형식 오류(page=abc)
     * - 상태/순서 위반(409)과 구분되는 "요청 형식 오류"로 해석한다.
     * - 메시지는 현재 단일 정책으로 "요청 값이 올바르지 않습니다."를 사용한다.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException e
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.ofBadRequest("요청 값이 올바르지 않습니다."));
    }

    private void tryLog409Failure(BusinessException e, HttpServletRequest request) {
        if (e.getStatus() != HttpStatus.CONFLICT) {
            return;
        }

        if (!(e instanceof AuditableConflictException ace)) {
            return;
        }

        ReasonCode reasonCode = ace.getReasonCode();
        if (reasonCode != ReasonCode.SAME_APPROVER_NOT_ALLOWED
                && reasonCode != ReasonCode.PAID_ALREADY) {
            return;
        }

        try {
            AuditLogCommand cmd = AuditLogCommand.builder()
                    .requestId(resolveRequestId(request))
                    .actorType(ace.getActorType())
                    .actorId(ace.getActorId())
                    .action(ace.getAction())
                    .entityType(ace.getEntityType())
                    .entityId(ace.getEntityId())
                    .statusBefore(ace.getStatusBefore())
                    .statusAfter(ace.getStatusAfter())
                    .merchantId(ace.getMerchantId())
                    .metaJson(AuditMetaFactory.requiredCommentSuccessWithReason(
                            reasonCode.name(),
                            ace.getComment()
                    ).toString())
                    .build();

            auditLogger.logFailureRequiresNew(cmd, reasonCode);
        } catch (Exception ex) {
            log.error("409 failure audit logging failed", ex);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        Object requestId = request.getAttribute(RequestIdKeys.ATTR_KEY);
        if (requestId instanceof String s && !s.isBlank()) {
            return s;
        }
        return request.getHeader(RequestIdKeys.HEADER);
    }
}