package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.dto.SettlementBatchRunResponse;
import com.settleops.domain.settlement.dto.SettlementPayActionResponse;
import com.settleops.domain.settlement.entity.Settlement;
import com.settleops.domain.settlement.infra.SettlementRepository;
import com.settleops.global.enums.ReasonCode;
import com.settleops.global.error.ConflictException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SettlementAdminCommandServiceImpl implements SettlementAdminCommandService {

    private final SettlementRepository settlementRepository;

    @Override
    public SettlementBatchRunResponse runBatch(LocalDate baseDate) {
        // PR#2(A4 only): A2 배치는 다음 PR에서 구현
        throw new ResponseStatusException(
                HttpStatus.NOT_IMPLEMENTED,
                "batch run (A2) will be implemented in the next PR."
        );
    }

    /**
     * A4: 지급 요청(4-eyes 1단계)
     * - no-op 200: 이미 PAY_REQUESTED면 현재 상태 그대로 반환
     * - 409: HOLD_ACTIVE / SETTLEMENT_NOT_READY
     * - requesterId는 인증 주체(SecurityContext)에서 추출
     */
    @Override
    @Transactional
    public SettlementPayActionResponse requestPaid(String settlementId, String comment) {
        // 1) 조회 (없으면 404)
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "settlement not found"));

        // 2) no-op 200: 이미 PAY_REQUESTED면 현재 상태 반환
        if (settlement.isPayRequested()) {
            return toPayActionResponse(settlement);
        }

        // 3) 409: HOLD_ACTIVE면 지급 요청 차단
        if (settlement.isHoldActive()) {
            throw new ConflictException(ReasonCode.HOLD_ACTIVE, "hold is active");
        }

        // 4) 409: READY 아니면 지급 요청 차단
        if (!settlement.isReady()) {
            throw new ConflictException(ReasonCode.SETTLEMENT_NOT_READY, "settlement is not READY");
        }

        // 5) 상태 전이: READY -> PAY_REQUESTED (+ 요청자/시각 기록)
        String requesterId = currentActorId();
        settlement.requestPaid(requesterId, LocalDateTime.now());

        // 6) 저장 + 응답
        settlementRepository.save(settlement);
        return toPayActionResponse(settlement);
    }

    /**
     * A4: 지급 승인(4-eyes 2단계)
     * LOCKED 동시성 순서:
     * 1) 이미 PAID면 즉시 no-op 200 반환(paidAt 필수)
     * 2) PAY_REQUESTED일 때만 락 조회 후 PAID 전이 1회 수행
     * - 409: PAY_REQUESTED_REQUIRED / SAME_APPROVER_NOT_ALLOWED
     */
    @Override
    @Transactional
    public SettlementPayActionResponse approvePaid(String settlementId, String comment) {
        // (1) 락 없이 조회: 없으면 404
        Settlement current = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "settlement not found"));

        // (1-1) no-op 200: 이미 PAID면 현재 상태 반환(paidAt은 toPayActionResponse에서 강제)
        if (current.isPaid()) {
            return toPayActionResponse(current);
        }

        // (2) 409: PAY_REQUESTED가 아니면 승인 불가
        if (!current.isPayRequested()) {
            throw new ConflictException(ReasonCode.PAY_REQUESTED_REQUIRED, "PAY_REQUESTED status required");
        }

        // (3) PAY_REQUESTED인 경우에만 락 조회(동시 PAID 전이 1회 보장)
        Settlement settlement = settlementRepository.findByIdForUpdate(settlementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "settlement not found"));

        // (3-1) 락 획득 후 재확인: 동시성으로 이미 PAID가 됐을 수 있음 → no-op 200
        if (settlement.isPaid()) {
            return toPayActionResponse(settlement);
        }

        String approverId = currentActorId();

        // (4) 409: 4-eyes 위반(요청자=승인자) 차단
        if (settlement.violatesFourEyes(approverId)) {
            throw new ConflictException(ReasonCode.SAME_APPROVER_NOT_ALLOWED, "requester and approver must be different");
        }

        // (5) 상태 전이: PAY_REQUESTED -> PAID (+ 승인자/시각 기록)
        settlement.approvePaid(approverId, LocalDateTime.now());
        settlementRepository.save(settlement);

        return toPayActionResponse(settlement);
    }

    /**
     * 운영 액션 응답 DTO 변환
     * - approve-paid no-op 200 규격: status=PAID이면 paidAt 필수(누락 시 데이터 손상으로 간주)
     */
    private SettlementPayActionResponse toPayActionResponse(Settlement s) {
        LocalDateTime paidAt = s.isPaid() ? s.getPaidApprovedAt() : null;
        if (s.isPaid() && paidAt == null) {
            throw new IllegalStateException("paidAt must not be null when status is PAID");
        }
        return new SettlementPayActionResponse(
                currentRequestId(),
                s.getSettlementId(),
                s.getStatus(),
                s.getPaidRequestedAt(),
                paidAt
        );
    }

    /** 인증 주체(현재 로그인 사용자) 식별자 */
    private String currentActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new IllegalStateException("actorId must not be null/blank");
        }
        return auth.getName();
    }

    /** RequestIdFilter가 MDC에 넣어준 requestId를 읽기만 한다(생성 금지) */
    private String currentRequestId() {
        return MDC.get("requestId");
    }
}