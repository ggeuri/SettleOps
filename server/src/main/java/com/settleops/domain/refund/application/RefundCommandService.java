package com.settleops.domain.refund.application;

import com.settleops.domain.refund.api.dto.RefundCreateRequestDTO;
import com.settleops.domain.refund.api.dto.RefundResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefundCommandService {

    // TODO: 나중에 repository 주입해서 실제 저장
    // private final RefundRepository refundRepository;
    // private final PaymentRepository paymentRepository;

    public RefundResponseDTO requestRefund(@Valid RefundCreateRequestDTO req) {
        // 지금은 최소 동작(가짜 응답)부터 만들기
        // 실제 구현 시: payment 조회/가드/저장/event insert 로 교체

        return RefundResponseDTO.builder()
                .refundId("RFD_TEMP")               // TODO: 저장 후 생성된 ID
                .paymentId(req.getPaymentId())
                .amount(req.getAmount())
                .status("REQUESTED")
                .requestedAt(java.time.LocalDateTime.now())
                .build();
    }
}
