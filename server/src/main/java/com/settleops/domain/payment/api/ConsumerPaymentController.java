package com.settleops.domain.payment.api;

import com.settleops.domain.payment.api.dto.ConfirmResponseDTO;
import com.settleops.domain.payment.application.ConfirmService;
import com.settleops.global.auth.annotation.LoginConsumer;
import com.settleops.global.error.UnauthorizedException;
import com.settleops.global.web.RequestIdResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/consumer/payments")
public class ConsumerPaymentController {

    private final ConfirmService confirmService;
    private final RequestIdResolver requestIdResolver;

    /**
     * 결제 확정(Confirm) API
     *
     * <p>구매자가 결제 승인 이후 거래 완료를 확정하는 Consumer 전용 API</p>
     *
     * <p>동작 방식</p>
     * <ul>
     *   <li>Payment 상태가 <b>CAPTURED</b>인 경우에만 confirm이 가능하다.</li>
     *   <li>confirm 시 payment.status는 변경하지 않고 <b>PAYMENT_CONFIRMED 이벤트</b>를 기록한다.</li>
     *   <li>정산(Settlement)의 SoT는 PAYMENT_CONFIRMED 이벤트이다.</li>
     * </ul>
     *
     * <p>멱등 처리</p>
     * <ul>
     *   <li>(payment_id, event_type) 유니크 제약을 이용해 PAYMENT_CONFIRMED 이벤트 중복 insert를 방지한다.</li>
     *   <li>이미 confirm 된 결제에 대해 재요청이 들어오면 동일 결과를 반환한다.</li>
     * </ul>
     *
     * <p>권한 검증</p>
     * <ul>
     *   <li>요청 사용자의 buyerId와 payment.buyer_id가 일치해야 한다.</li>
     *   <li>buyerId 누락 시 <b>401 UNAUTHORIZED</b>를 반환한다.</li>
     *   <li>불일치 시 <b>403 BUYER_MISMATCH</b>를 반환한다.</li>
     * </ul>
     *
     * <p>응답</p>
     * <pre>
     * {
     *   "paymentId": "...",
     *   "orderId": "...",
     *   "status": "CAPTURED",
     *   "confirmedAt": "2026-03-04T12:00:00"
     * }
     * </pre>
     *
     * <p>기획서 정합성</p>
     * <ul>
     *   <li>정산 SoT = PAYMENT_CONFIRMED 이벤트</li>
     *   <li>payment.status는 CAPTURED 유지</li>
     *   <li>Consumer만 confirm 가능</li>
     * </ul>
     */
    @PostMapping("/{paymentId}/confirm")
    public ConfirmResponseDTO confirm(
            @PathVariable String paymentId,
            HttpServletRequest request,
            @LoginConsumer String loginConsumerId
    ) {
        if (loginConsumerId == null||loginConsumerId.isBlank()) {
            throw new UnauthorizedException("인증이 필요합니다.");
        }

        String requestId = requestIdResolver.resolve(request);

        return confirmService.confirm(paymentId, loginConsumerId, requestId);
    }

}
