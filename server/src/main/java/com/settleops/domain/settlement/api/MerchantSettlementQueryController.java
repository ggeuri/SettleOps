package com.settleops.domain.settlement.api;

import com.settleops.domain.settlement.application.MerchantSettlementQueryService;
import com.settleops.domain.settlement.dto.MerchantSettlementDetailResponse;
import com.settleops.domain.settlement.dto.MerchantSettlementListItemResponse;
import com.settleops.domain.settlement.enums.SettlementStatus;
import com.settleops.global.auth.annotation.LoginMerchant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/merchants/{merchantId}/settlements")
public class MerchantSettlementQueryController {

    private final MerchantSettlementQueryService merchantSettlementQueryService;

    @GetMapping
    public ResponseEntity<Page<MerchantSettlementListItemResponse>> getMerchantSettlements(
            @PathVariable String merchantId,
            @RequestParam(required = false) SettlementStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable,
            @LoginMerchant String loginMerchantId
    ) {
        Pageable fixedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        return ResponseEntity.ok(
                merchantSettlementQueryService.getMerchantSettlements(
                        loginMerchantId,
                        merchantId,
                        status,
                        from,
                        to,
                        fixedPageable
                )
        );
    }

    @GetMapping("/{settlementId}")
    public ResponseEntity<MerchantSettlementDetailResponse> getMerchantSettlementDetail(
            @PathVariable String merchantId,
            @PathVariable String settlementId,
            @LoginMerchant String loginMerchantId
    ) {
        return ResponseEntity.ok(
                merchantSettlementQueryService.getMerchantSettlementDetail(
                        loginMerchantId,
                        merchantId,
                        settlementId
                )
        );
    }
}