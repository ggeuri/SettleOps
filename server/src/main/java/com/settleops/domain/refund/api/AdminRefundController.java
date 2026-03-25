// 수정 파일: server/src/main/java/com/settleops/domain/refund/api/AdminRefundController.java

package com.settleops.domain.refund.api;

import com.settleops.domain.refund.api.dto.AdminRefundDecisionRequestDTO;
import com.settleops.domain.refund.api.dto.AdminRefundDecisionResponseDTO;
import com.settleops.domain.refund.api.dto.AdminRefundListItemDTO;
import com.settleops.domain.refund.api.dto.RefundTraceEntryResponseDTO;
import com.settleops.domain.refund.application.RefundAdminQueryService;
import com.settleops.domain.refund.application.RefundAdminService;
import com.settleops.domain.refund.application.RefundTraceEntryQueryService;
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
    private final RefundTraceEntryQueryService refundTraceEntryQueryService;

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

    @GetMapping("/{refundId}/trace-entry")
    public ResponseEntity<RefundTraceEntryResponseDTO> getRefundTraceEntry(
            @PathVariable String refundId
    ) {
        return ResponseEntity.ok(
                refundTraceEntryQueryService.getRefundTraceEntry(refundId)
        );
    }

    @PatchMapping("/{refundId}/approve")
    public ResponseEntity<AdminRefundDecisionResponseDTO> approve(
            @PathVariable String refundId,
            @Valid @RequestBody AdminRefundDecisionRequestDTO request,
            @LoginAdmin String adminId,
            HttpServletRequest httpServletRequest
    ) {
        String requestId = requestIdResolver.resolve(httpServletRequest);

        return ResponseEntity.ok(
                refundAdminService.approve(refundId, adminId, request.getComment(), requestId)
        );
    }

    @PatchMapping("/{refundId}/reject")
    public ResponseEntity<AdminRefundDecisionResponseDTO> reject(
            @PathVariable String refundId,
            @Valid @RequestBody AdminRefundDecisionRequestDTO request,
            @LoginAdmin String adminId,
            HttpServletRequest httpServletRequest
    ) {
        String requestId = requestIdResolver.resolve(httpServletRequest);

        return ResponseEntity.ok(
                refundAdminService.reject(refundId, adminId, request.getComment(), requestId)
        );
    }
}