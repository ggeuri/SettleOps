package com.settleops.domain.refund.api;

import com.settleops.domain.refund.api.dto.AdminRefundDecisionRequestDTO;
import com.settleops.domain.refund.api.dto.AdminRefundDecisionResponseDTO;
import com.settleops.domain.refund.api.dto.AdminRefundListItemDTO;
import com.settleops.domain.refund.application.RefundAdminQueryService;
import com.settleops.domain.refund.application.RefundAdminService;
import com.settleops.global.auth.annotation.LoginAdmin;
import com.settleops.global.web.RequestIdResolver;
import com.settleops.global.web.pagination.PageResponse;
import com.settleops.global.web.pagination.PageableUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/refunds")
public class AdminRefundController {

    private final RefundAdminService refundAdminService;
    private final RefundAdminQueryService refundAdminQueryService;
    private final RequestIdResolver requestIdResolver;

    @GetMapping
    public ResponseEntity<PageResponse<AdminRefundListItemDTO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String settlementId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "requestedAt") String sortKey,
            @RequestParam(required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size
    ) {
        Pageable pageable = PageableUtils.validateAndCreate(page, size);

        Page<AdminRefundListItemDTO> result = refundAdminQueryService.list(
                status,
                from,
                to,
                settlementId,
                keyword,
                sortKey,
                sortDirection,
                pageable
        );

        return ResponseEntity.ok(PageResponse.from(result));
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
            @LoginAdmin String adminId,
            HttpServletRequest request
    ) {
        String requestId = requestIdResolver.resolve(request);

        return ResponseEntity.ok(
                refundAdminService.reject(refundId, adminId, req.getComment(), requestId)
        );
    }
}