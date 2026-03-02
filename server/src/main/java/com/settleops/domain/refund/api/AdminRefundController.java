package com.settleops.domain.refund.api;

import com.settleops.domain.refund.api.dto.AdminRefundDecisionRequestDTO;
import com.settleops.domain.refund.api.dto.AdminRefundDecisionResponseDTO;
import com.settleops.domain.refund.application.RefundAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/refunds")
public class AdminRefundController {
    //GET /api/admin/refunds?status=
    //PATCH /api/admin/refunds/{refundId}/approve
    //PATCH /api/admin/refunds/{refundId}/reject

    private final RefundAdminService refundAdminService;

    @PatchMapping("/{refundId}/approve")
    public ResponseEntity<AdminRefundDecisionResponseDTO> approve(
            @PathVariable String refundId,
            @RequestBody @Valid AdminRefundDecisionRequestDTO req,
            Authentication authentication
    ) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String adminId = userDetails.getUsername();
        return ResponseEntity.ok(refundAdminService.approve(refundId, adminId, req.getComment()));
    }

    @PatchMapping("/{refundId}/reject")
    public ResponseEntity<AdminRefundDecisionResponseDTO> reject(
            @PathVariable String refundId,
            @RequestBody @Valid AdminRefundDecisionRequestDTO req,
            Authentication authentication
    ) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String adminId = userDetails.getUsername();
        return ResponseEntity.ok(refundAdminService.reject(refundId, adminId, req.getComment()));
    }

}
