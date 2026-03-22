package com.settleops.domain.order.api;

import com.settleops.domain.order.api.dto.OrderCreateRequestDTO;
import com.settleops.domain.order.api.dto.OrderCreateResponseDTO;
import com.settleops.domain.order.application.ConsumerOrderFacade;
import com.settleops.domain.order.domain.Orders;
import com.settleops.global.web.RequestIdResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/consumer/orders")
public class ConsumerOrderController {

    private final ConsumerOrderFacade consumerOrderFacade;
    private final RequestIdResolver requestIdResolver;

    @PostMapping
    public ResponseEntity<OrderCreateResponseDTO> createOrder(
            @RequestBody @Valid OrderCreateRequestDTO request,
            HttpServletRequest httpServletRequest
    ) {
        String requestId = requestIdResolver.resolve(httpServletRequest);

        Orders order = consumerOrderFacade.createOrderWithPaymentCreated(
                request.getMerchantId(),
                request.getBuyerId(),
                request.getItemName(),
                request.getAmount(),
                requestId
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(OrderCreateResponseDTO.from(order));
    }
}