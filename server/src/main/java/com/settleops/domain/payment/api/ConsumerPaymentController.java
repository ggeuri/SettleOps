package com.settleops.domain.payment.api;

import com.settleops.domain.payment.api.dto.PayResponseDTO;
import com.settleops.domain.payment.application.ConfirmService;
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

    /** .. */
    @PostMapping("/{paymentId}/confirm")
    public PayResponseDTO confirm(@PathVariable String paymentId) {
        return confirmService.confirm(paymentId);
    }

}
