package com.settleops.domain.refund.api;

import com.settleops.domain.refund.api.dto.AdminRefundDecisionRequestDTO;
import com.settleops.domain.refund.api.dto.AdminRefundDecisionResponseDTO;
import com.settleops.domain.refund.api.dto.AdminRefundListItemDTO;
import com.settleops.domain.refund.application.RefundAdminQueryService;
import com.settleops.domain.refund.application.RefundAdminService;
import com.settleops.global.auth.annotation.LoginAdmin;
import com.settleops.global.web.RequestIdResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/refunds")
public class AdminRefundController {
    //GET /api/admin/refunds?status=&from=&to=
    //PATCH /api/admin/refunds/{refundId}/approve
    //PATCH /api/admin/refunds/{refundId}/reject

    private final RefundAdminService refundAdminService;
    private final RefundAdminQueryService refundAdminQueryService;
    private final RequestIdResolver requestIdResolver;

    /**
     * A6 운영 큐 조회(READ).
     * - status/from/to는 옵션.
     * - 기준 시각: requestedAt (DB requested_at)
     */
    @GetMapping
    public ResponseEntity<List<AdminRefundListItemDTO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(refundAdminQueryService.list(status, from, to));
    }

    @PatchMapping("/{refundId}/approve")
    public ResponseEntity<AdminRefundDecisionResponseDTO> approve(
            @PathVariable String refundId,
            @RequestBody @Valid AdminRefundDecisionRequestDTO req,
            @LoginAdmin String adminId,
            HttpServletRequest request
    ) {
        String requestId = requestIdResolver.resolve(request);
        return ResponseEntity.ok(
                refundAdminService.approve(refundId, adminId, req.getComment(), requestId)
        );
    }

    @PatchMapping("/{refundId}/reject")
    public ResponseEntity<AdminRefundDecisionResponseDTO> reject(
            @PathVariable String refundId,
            @RequestBody @Valid AdminRefundDecisionRequestDTO req,
            HttpServletRequest request,
            @LoginAdmin String adminId
    ) {
        String requestId = requestIdResolver.resolve(request);
        return ResponseEntity.ok(
                refundAdminService.reject(refundId, adminId, req.getComment(), requestId)
        );
    }

}
