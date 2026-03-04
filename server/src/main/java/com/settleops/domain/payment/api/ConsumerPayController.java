package com.settleops.domain.payment.api;

import com.settleops.domain.payment.api.dto.PayResponseDTO;
import com.settleops.domain.payment.application.PayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/consumer/orders")
public class ConsumerPayController {

    private final PayService payService;

    /** 주문 상세 조회 */
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderDetail( // <OrderDetailResponseDTO>
            @PathVariable String orderId
    ){
        // payService.findOrderDetail();
        return null;
    }

    /** 결제하기 */
    @PostMapping("/{orderId}/pay")
    public ResponseEntity<PayResponseDTO> pay(
            @PathVariable String orderId,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey
    ) {
        PayResponseDTO response =
                payService.pay(orderId, idempotencyKey);

        return ResponseEntity.ok(response);
    }

}
