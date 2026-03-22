package com.settleops.domain.order.api;

import com.settleops.domain.order.api.dto.OrderCreateRequestDTO;
import com.settleops.domain.order.api.dto.OrderCreateResponseDTO;
import com.settleops.domain.order.application.ConsumerOrderFacade;
import com.settleops.domain.order.domain.Orders;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/consumer/orders")
public class ConsumerOrderController {

    private final ConsumerOrderFacade consumerOrderFacade;

    @PostMapping
    public ResponseEntity<OrderCreateResponseDTO> createOrder(
            @RequestBody @Valid OrderCreateRequestDTO request,
            HttpServletRequest httpServletRequest
    ) {

        Orders order = consumerOrderFacade.createOrderWithPaymentCreated(
                request.getMerchantId(),
                request.getBuyerId(),
                request.getItemName(),
                request.getAmount()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(OrderCreateResponseDTO.from(order));
    }
}