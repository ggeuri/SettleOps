package com.settleops.domain.refund.application;

import com.settleops.domain.refund.api.dto.RefundRowDTO;
import com.settleops.domain.refund.domain.Refund;
import com.settleops.domain.refund.infra.RefundReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundQueryServiceImpl implements RefundQueryService {

    private final RefundReadRepository refundReadRepository;

    @Override
    public List<RefundRowDTO> myRefunds() {
        List<Refund> refunds = refundReadRepository.findAllByOrderByCreatedAtDesc();

        return refunds.stream()
                .map(r -> new RefundRowDTO(
                        r.getRefundId(),
                        r.getPaymentId(),
                        r.getMerchantId(),
                        r.getAmount(),
                        r.getStatus(),
                        r.getReasonText(),
                        r.getRequestedAt(),
                        r.getDecidedAt()
                ))
                .toList();
    }
}