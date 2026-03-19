package com.settleops.domain.hold.api;

import com.settleops.domain.hold.api.dto.HoldActionRequest;
import com.settleops.domain.hold.api.dto.HoldCreateRequest;
import com.settleops.domain.hold.application.HoldApproveCommand;
import com.settleops.domain.hold.application.HoldCreateCommand;
import com.settleops.domain.hold.application.HoldCreateResponse;
import com.settleops.domain.hold.application.HoldDecisionResponse;
import com.settleops.domain.hold.application.HoldReleaseCommand;
import com.settleops.domain.hold.application.HoldService;
import com.settleops.global.auth.annotation.LoginAdmin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RestController
public class AdminHoldController {

    private final HoldService holdService;

    /**
     * 현재는 MVP 스켈레톤 검증용으로 adminId를 고정값("ADMIN")으로 주입한다.
     * 추후 세션 식별자 정책 확정 후 실제 adminId/username 주입으로 교체한다.
     */
    @PostMapping("/holds")
    public ResponseEntity<HoldCreateResponse> createHold(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @LoginAdmin String adminId,
            @Valid @RequestBody HoldCreateRequest request
    ) {
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
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @LoginAdmin String adminId,
            @Valid @RequestBody HoldActionRequest request
    ) {
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
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @LoginAdmin String adminId,
            @Valid @RequestBody HoldActionRequest request
    ) {
        HoldReleaseCommand command = new HoldReleaseCommand(
                requestId,
                adminId,
                request.comment()
        );

        return ResponseEntity.ok(holdService.releaseHold(holdId, command));
    }
}