package com.settleops.domain.hold.api;

import com.settleops.domain.hold.api.dto.AdminHoldQueueResponse;
import com.settleops.domain.hold.api.dto.HoldActionRequest;
import com.settleops.domain.hold.api.dto.HoldCreateRequest;
import com.settleops.domain.hold.api.dto.HoldQueueSearchRequestDto;
import com.settleops.domain.hold.application.HoldApproveCommand;
import com.settleops.domain.hold.application.HoldCreateCommand;
import com.settleops.domain.hold.application.HoldCreateResponse;
import com.settleops.domain.hold.application.HoldDecisionResponse;
import com.settleops.domain.hold.application.HoldQueryService;
import com.settleops.domain.hold.application.HoldReleaseCommand;
import com.settleops.domain.hold.application.HoldService;
import com.settleops.domain.hold.domain.HoldStatus;
import com.settleops.global.auth.annotation.LoginAdmin;
import com.settleops.global.web.RequestIdResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RestController
public class AdminHoldController {

    private final HoldService holdService;
    private final HoldQueryService holdQueryService;
    private final RequestIdResolver requestIdResolver;

    @GetMapping("/holds")
    public ResponseEntity<AdminHoldQueueResponse> searchHolds(
            @RequestParam(required = false) HoldStatus status,
            @RequestParam(required = false) String settlementId,
            @RequestParam(required = false) String merchantId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        HoldQueueSearchRequestDto request = new HoldQueueSearchRequestDto(
                status,
                settlementId,
                merchantId
        );

        return ResponseEntity.ok(holdQueryService.search(request, pageable));
    }

    @PostMapping("/holds")
    public ResponseEntity<HoldCreateResponse> createHold(
            HttpServletRequest httpServletRequest,
            @LoginAdmin String adminId,
            @Valid @RequestBody HoldCreateRequest request
    ) {
        String requestId = requestIdResolver.resolve(httpServletRequest);

        HoldCreateCommand command = new HoldCreateCommand(
                requestId,
                adminId,
                request.settlementId(),
                request.reasonCode(),
                request.comment()
        );

        return ResponseEntity.ok(holdService.createHold(command));
    }

    @PatchMapping("/holds/{holdId}/approve")
    public ResponseEntity<HoldDecisionResponse> approveHold(
            @PathVariable String holdId,
            HttpServletRequest httpServletRequest,
            @LoginAdmin String adminId,
            @Valid @RequestBody HoldActionRequest request
    ) {
        String requestId = requestIdResolver.resolve(httpServletRequest);

        HoldApproveCommand command = new HoldApproveCommand(
                requestId,
                adminId,
                request.comment()
        );

        return ResponseEntity.ok(holdService.approveHold(holdId, command));
    }

    @PatchMapping("/holds/{holdId}/release")
    public ResponseEntity<HoldDecisionResponse> releaseHold(
            @PathVariable String holdId,
            HttpServletRequest httpServletRequest,
            @LoginAdmin String adminId,
            @Valid @RequestBody HoldActionRequest request
    ) {
        String requestId = requestIdResolver.resolve(httpServletRequest);

        HoldReleaseCommand command = new HoldReleaseCommand(
                requestId,
                adminId,
                request.comment()
        );

        return ResponseEntity.ok(holdService.releaseHold(holdId, command));
    }
}