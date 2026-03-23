package com.settleops.domain.refund.api;

import com.settleops.domain.refund.api.dto.RefundCreateRequestDTO;
import com.settleops.domain.refund.api.dto.RefundResponseDTO;
import com.settleops.domain.refund.api.dto.RefundRowDTO;
import com.settleops.domain.refund.application.RefundCommandService;
import com.settleops.domain.refund.application.RefundQueryService;
import com.settleops.global.web.RequestIdResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<List<RefundRowDTO>> myRefunds() {
        return ResponseEntity.ok(refundQueryService.myRefunds());
    }
}