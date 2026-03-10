package com.settleops.domain.payment.application;

import com.settleops.domain.payment.api.dto.ConfirmResponseDTO;
import com.settleops.domain.payment.domain.Payment;
import com.settleops.domain.payment.domain.PaymentStatus;
import com.settleops.domain.payment.infra.PaymentRepository;
import com.settleops.global.audit.ActorType;
import com.settleops.global.audit.AuditLogCommand;
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.audit.EntityType;
import com.settleops.global.enums.Action;
import com.settleops.global.enums.ReasonCode;
import com.settleops.global.error.BadRequestException;
import com.settleops.global.error.ConflictException;
import com.settleops.global.error.ForbiddenException;
import com.settleops.global.error.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ConfirmService {

    private final PaymentRepository paymentRepository;
    private final ConfirmEventWriter confirmEventWriter;
    private final ConfirmQueryService confirmQueryService;
    private final AuditLogger auditLogger;

    /**
     * 결제 confirm 처리
     *
     * <p>정책
     * <br>- CONFIRMED의 SoT는 payment.status가 아니라 PAYMENT_CONFIRMED 이벤트입니다.
     * <br>- payment.status는 CAPTURED로 유지합니다.
     * <br>- 멱등은 (payment_id, event_type) UNIQUE 제약으로 최종 보장합니다.
     * </p>
     *
     * <p>구현 메모
     * <br>- 일반 재시도는 선조회로 no-op 처리합니다.
     * <br>- 동시 요청 충돌은 insert 전용 writer에서 처리합니다.
     * <br>- confirmedAt은 항상 PAYMENT_CONFIRMED.occurred_at 기준으로 반환합니다.
     * </p>
     */
    @Transactional
    public ConfirmResponseDTO confirm(String paymentId, String currentBuyerId, String requestId) {

        validateRequestId(requestId);

        // 1) 결제 조회
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BadRequestException("존재하지 않는 결제입니다."));

        // 2) 선행조건 및 권한 검증
        validateConfirmable(payment, currentBuyerId);

        // 3) 이미 CONFIRMED면 즉시 no-op 수렴
        LocalDateTime existingConfirmedAt = confirmQueryService.findConfirmedAt(paymentId).orElse(null);
        if (existingConfirmedAt != null) {
            logConfirm(
                    payment,
                    currentBuyerId,
                    requestId,
                    existingConfirmedAt,
                    true,
                    "ALREADY_CONFIRMED"
            );
            return ConfirmResponseDTO.from(payment, existingConfirmedAt);
        }

        // 4) CONFIRMED 이벤트 insert 시도
        // - true  : 이번 요청이 최초 confirm 성공
        // - false : 동시 요청으로 이미 다른 트랜잭션이 선점
        boolean inserted = confirmEventWriter.tryInsertConfirmed(paymentId, requestId);

        // 5) 최종 confirmedAt은 항상 SoT(event.occurred_at) 재조회로 확정
        LocalDateTime confirmedAt = confirmQueryService.getConfirmedAtOrThrow(paymentId);

        // 6) 감사 로그
        logConfirm(
                payment,
                currentBuyerId,
                requestId,
                confirmedAt,
                !inserted,
                inserted ? null : "ALREADY_CONFIRMED"
        );

        return ConfirmResponseDTO.from(payment, confirmedAt);
    }

    /**
     * requestId는 추적 / 감사 로그 기준값이므로 필수입니다.
     * 누락 시 내부 오류가 아니라 잘못된 요청으로 처리합니다.
     */
    private void validateRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw new BadRequestException("X-Request-Id는 필수입니다.");
        }
    }

    /**
     * confirm 가능 여부 및 호출 주체를 검증합니다.
     */
    private void validateConfirmable(Payment payment, String currentBuyerId) {

        // confirm은 CAPTURED 상태에서만 허용
        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            throw new ConflictException(
                    ReasonCode.PAYMENT_NOT_CAPTURED,
                    "CAPTURED 상태에서만 CONFIRM 가능합니다."
            );
        }

        // 인증 주체 없음
        if (currentBuyerId == null || currentBuyerId.isBlank()) {
            throw new UnauthorizedException("UNAUTHORIZED");
        }

        String buyerId = payment.getBuyerId();

        // 내부 데이터 정합성 오류
        if (buyerId == null || buyerId.isBlank()) {
            throw new IllegalStateException("payment.buyer_id is null/blank");
        }

        // 구매자 불일치
        if (!buyerId.equals(currentBuyerId)) {
            throw new ForbiddenException("BUYER_MISMATCH");
        }
    }

    /**
     * confirm 감사 로그
     *
     * <p> @param idempotentReplay 멱등 재시도/no-op 여부
     * <br> @param noOpReason       no-op 사유. 최초 성공이면 null
     * </p>
     *
     * <p>주의
     * <br>- confirm의 SoT는 PAYMENT_CONFIRMED 이벤트이며,
     * <br>- payment.status는 변경되지 않으므로 before/after 모두 CAPTURED입니다.
     * </p>
     */
    private void logConfirm(
            Payment payment,
            String currentBuyerId,
            String requestId,
            LocalDateTime confirmedAt,
            boolean idempotentReplay,
            String noOpReason
    ) {
        auditLogger.log(
                AuditLogCommand.builder()
                        .requestId(requestId)
                        .action(Action.PAYMENT_CONFIRMED)
                        .actorType(ActorType.BUYER)
                        .actorId(currentBuyerId)
                        .entityType(EntityType.PAYMENT)
                        .entityId(payment.getPaymentId())
                        .statusBefore(PaymentStatus.CAPTURED.name())
                        .statusAfter(PaymentStatus.CAPTURED.name())
                        .merchantId(payment.getMerchantId())
                        .occurredAt(confirmedAt)
                        .metaJson(buildConfirmMetaJson(idempotentReplay, noOpReason))
                        .build()
        );
    }

    private String buildConfirmMetaJson(boolean idempotentReplay, String noOpReason) {
        boolean noOp = noOpReason != null && !noOpReason.isBlank();

        if (!noOp) {
            return "{\"noOp\":false,\"idempotent\":" + idempotentReplay + "}";
        }

        return "{\"noOp\":true,\"noOpReason\":\"" + noOpReason + "\",\"idempotent\":" + idempotentReplay + "}";
    }
}