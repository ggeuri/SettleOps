package com.settleops.domain.settlement.api;

import com.settleops.domain.settlement.application.SettlementDetailQueryService;
import com.settleops.domain.settlement.application.SettlementQueryService;
import com.settleops.domain.settlement.dto.AdminSettlementDetailResponse;
import com.settleops.domain.settlement.dto.AdminSettlementListItemResponse;
import com.settleops.domain.settlement.enums.SettlementStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/settlements")
public class AdminSettlementQueryController {

    private final SettlementQueryService settlementQueryService;
    private final SettlementDetailQueryService settlementDetailQueryService;

    @GetMapping
    public ResponseEntity<Page<AdminSettlementListItemResponse>> getSettlements(
            @RequestParam(required = false)SettlementStatus status,
            @RequestParam(required = false) String merchantId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Pageable fixedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );
        return ResponseEntity.ok(
                settlementQueryService.getAdminSettlements(status, merchantId, fixedPageable)
        );
    }

    @GetMapping("/{settlementId}")
    public ResponseEntity<AdminSettlementDetailResponse> getSettlementDetail(
            @PathVariable String settlementId
    ) {
        return ResponseEntity.ok(settlementDetailQueryService.getAdminSettlementDetail(settlementId));
    }
}
