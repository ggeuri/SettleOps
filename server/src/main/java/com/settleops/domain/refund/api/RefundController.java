package com.settleops.domain.refund.api;

import com.settleops.domain.refund.api.dto.AdminRefundListItemDTO;
import com.settleops.domain.refund.api.dto.RefundCreateRequestDTO;
import com.settleops.domain.refund.api.dto.RefundResponseDTO;
import com.settleops.domain.refund.application.RefundCommandService;
import com.settleops.domain.refund.application.RefundQueryServiceImpl;
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
    private final RefundQueryServiceImpl refundQueryServiceImpl;
    private final RequestIdResolver requestIdResolver;

    //POST /api/refunds (merchant 환불 요청) (U6)
    @PostMapping("/refunds")
    public ResponseEntity<RefundResponseDTO> requestRefund(
            @RequestBody @Valid RefundCreateRequestDTO req,
            HttpServletRequest request
    ) {
        String requestId = requestIdResolver.resolve(request);
        return ResponseEntity.ok(refundCommandService.requestRefund(req, requestId));
    }

    // 브라우저로 들어가서 JSON 보기용 (U6 조회)
    @GetMapping("/me/refunds")
    public ResponseEntity<List<AdminRefundListItemDTO>> myRefunds() {
        return ResponseEntity.ok(refundQueryServiceImpl.myRefunds());
    }

}
