package com.settleops.domain.refund.api;

import com.settleops.domain.refund.api.dto.RefundCreateRequestDTO;
import com.settleops.domain.refund.api.dto.RefundResponseDTO;
import com.settleops.domain.refund.api.dto.RefundRowDTO;
import com.settleops.domain.refund.application.RefundCommandService;
import com.settleops.domain.refund.application.RefundQueryService;
import com.settleops.global.auth.annotation.LoginMerchant;
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
@RequestMapping("/api")
public class RefundController {

    private final RefundCommandService refundCommandService;
    private final RefundQueryService refundQueryService;
    private final RequestIdResolver requestIdResolver;

    @PostMapping("/refunds")
    public ResponseEntity<RefundResponseDTO> requestRefund(
            @RequestBody @Valid RefundCreateRequestDTO req,
            HttpServletRequest request
    ) {
        String requestId = requestIdResolver.resolve(request);
        return ResponseEntity.ok(refundCommandService.requestRefund(req, requestId));
    }

    @GetMapping("/me/refunds")
    public ResponseEntity<PageResponse<RefundRowDTO>> myRefunds(
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "requestedAt") String sortKey,
            @RequestParam(required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @LoginMerchant String loginMerchantId
    ) {
        Pageable pageable = PageableUtils.validateAndCreate(page, size);

        Page<RefundRowDTO> result = refundQueryService.myRefunds(
                loginMerchantId,
                status,
                from,
                to,
                keyword,
                sortKey,
                sortDirection,
                pageable
        );

        return ResponseEntity.ok(PageResponse.from(result));
    }
}