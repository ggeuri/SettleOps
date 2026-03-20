package com.settleops.domain.settlement.api;

import com.settleops.domain.settlement.application.SettlementAdminCommandService;
import com.settleops.domain.settlement.dto.SettlementBatchRunResponse;
import com.settleops.domain.settlement.dto.SettlementPayActionRequest;
import com.settleops.domain.settlement.dto.SettlementPayActionResponse;
import com.settleops.global.auth.annotation.LoginAdmin;
import com.settleops.global.web.RequestIdResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin")
public class AdminSettlementCommandController {
    private final SettlementAdminCommandService commandService;
    private final RequestIdResolver requestIdResolver;

    public AdminSettlementCommandController(
            SettlementAdminCommandService commandService,
            RequestIdResolver requestIdResolver
    ) {
        this.commandService = commandService;
        this.requestIdResolver = requestIdResolver;
    }

    /**
     * A2 배치 실행·이력(운영 콘솔)
     * POST /api/admin/settlement-batches/run?baseDate=YYYY-MM-DD

     * LOCKED:
     * - baseDate 동일 재실행은 SKIP + 기존 run_id/result 반환
     * - requestId는 운영 응답이므로 body 포함 OK
     */
    @PostMapping("/settlement-batches/run")
    public ResponseEntity<SettlementBatchRunResponse> runBatch(
            @RequestParam("baseDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate baseDate,
            HttpServletRequest request,
            @LoginAdmin String adminId
    ) {
        String requestId = extractRequestId(request);
        return ResponseEntity.ok(commandService.runBatch(baseDate, requestId, adminId));
    }

    /**
     * A4: 지급 요청(4-eyes 1단계)
     * PATCH /api/admin/settlements/{settlementId}/request-paid

     * LOCKED:
     * - READY 아니면 409 (SETTLEMENT_NOT_READY/HOLD_ACTIVE/BATCH_FAILED/REFUND_ADJUSTMENT_PENDING)
     * - 이미 PAY_REQUESTED면 no-op 200 + 현재 상태 반환
     */
    @PatchMapping("/settlements/{settlementId}/request-paid")
    public ResponseEntity<SettlementPayActionResponse> requestPaid(
            @PathVariable String settlementId,
            @RequestBody(required = false) @Valid SettlementPayActionRequest body,
            HttpServletRequest request,
            @LoginAdmin String adminId
    ) {
        String comment = (body == null) ? null : body.comment();
        String requestId = extractRequestId(request);
        return ResponseEntity.ok(commandService.requestPaid(settlementId, comment, requestId, adminId));
    }

    /**
     * A4: 지급 승인(4-eyes 2단계)
     * PATCH /api/admin/settlements/{settlementId}/approve-paid

     * LOCKED:
     * - 이미 PAID면 no-op 200 + (status=PAID + paidAt) 필수
     * - PAY_REQUESTED가 아니면 409 PAY_REQUESTED_REQUIRED
     * - 요청자=승인자면 409 SAME_APPROVER_NOT_ALLOWED
     * - (옵션) 동시성 락 실패는 409 IN_PROGRESS
     */
    @PatchMapping("/settlements/{settlementId}/approve-paid")
    public ResponseEntity<SettlementPayActionResponse> approvePaid(
            @PathVariable String settlementId,
            @RequestBody(required = false) @Valid SettlementPayActionRequest body,
            HttpServletRequest request,
            @LoginAdmin String adminId
    ) {
        String comment = (body == null) ? null : body.comment();
        String requestId = extractRequestId(request);
        return ResponseEntity.ok(commandService.approvePaid(settlementId, comment, requestId, adminId));
    }

    private String extractRequestId(HttpServletRequest request) {
        return requestIdResolver.resolve(request);
    }
}