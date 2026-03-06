package com.settleops.domain.refund.application;

import com.settleops.domain.refund.api.dto.AdminRefundListItemDTO;
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
    public List<AdminRefundListItemDTO> myRefunds() {
        // 1) 우선 개발 확인용: 전체 조회(최근순)
        List<Refund> refunds = refundReadRepository.findAllByOrderByCreatedAtDesc();

        // 2) 엔티티 -> DTO 매핑 (AdminRefundListItemDTO 필드에 맞춰 수정)
        return refunds.stream()
                .map(r -> new AdminRefundListItemDTO(
                        r.getRefundId(),
                        r.getPaymentId(),
                        r.getMerchantId(),
                        r.getAmount(),
                        r.getStatus(),
                        r.getRequestedAt(),
                        r.getDecidedAt()
                ))
                .toList();
    }
}