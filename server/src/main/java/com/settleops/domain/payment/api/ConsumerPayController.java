package com.settleops.domain.payment.api;

import com.settleops.domain.payment.api.dto.PayResponseDTO;
import com.settleops.domain.payment.application.PayService;
import com.settleops.global.error.BadRequestException;
import com.settleops.global.logging.RequestIdKeys;
import com.settleops.global.web.RequestIdResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/consumer/orders")
public class ConsumerPayController {

    private final PayService payService;
    private final RequestIdResolver requestIdResolver;

    /** 주문 상세 조회 */
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderDetail( // <OrderDetailResponseDTO>
            @PathVariable String orderId
    ){
        // payService.findOrderDetail();
        return ResponseEntity.status(501).build();
    }

    /**
     * 결제 실행 API
     *
     * <p>주문(orderId)에 대한 결제를 생성하고 즉시 승인(CAPTURE)까지 수행한다.</p>
     *
     * <p>요청 헤더</p>
     * <ul>
     *   <li><b>X-Idempotency-Key</b> : 결제 요청의 멱등성을 보장하기 위한 키</li>
     * </ul>
     *
     * <p>동작 방식</p>
     * <ul>
     *   <li>동일한 X-Idempotency-Key로 요청이 재시도될 경우 중복 결제가 발생하지 않도록 처리한다.</li>
     *   <li>서버는 idempotency_record 테이블을 통해 요청 중복 여부를 검증한다.</li>
     * </ul>
     *
     * <p>응답</p>
     * <pre>
     * {
     *   "paymentId": "...",
     *   "orderId": "...",
     *   "status": "CAPTURED",
     *   "capturedAmount": 10000,
     *   "capturedAt": "2026-03-04T12:00:00"
     * }
     * </pre>
     *
     * <p>기획서 정합성</p>
     * <ul>
     *   <li>결제 API는 멱등 키 기반 재시도 안전성을 보장해야 한다.</li>
     *   <li>클라이언트는 반드시 X-Idempotency-Key 헤더를 포함해야 한다.</li>
     * </ul>
     */
    @PostMapping("/{orderId}/pay")
    public ResponseEntity<PayResponseDTO> pay(
            @PathVariable String orderId,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            HttpServletRequest request
    ) {

        String requestId = requestIdResolver.resolve(request);

        PayResponseDTO response =
                payService.pay(orderId, idempotencyKey,requestId);

        return ResponseEntity.ok(response);
    }

}
