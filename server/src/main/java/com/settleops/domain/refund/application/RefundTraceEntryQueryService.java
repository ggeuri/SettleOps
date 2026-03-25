package com.settleops.domain.refund.application;

import com.settleops.domain.refund.api.dto.RefundTraceEntryResponseDTO;
import com.settleops.domain.refund.infra.RefundTraceQueryRepository;
import com.settleops.global.error.BadRequestException;
import com.settleops.global.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundTraceEntryQueryService {

    private final RefundTraceQueryRepository refundTraceQueryRepository;

    public RefundTraceEntryResponseDTO getRefundTraceEntry(String refundId) {

        if (refundId == null || refundId.isBlank()) {
            throw new BadRequestException("refundId must not be null/blank");
        }

        if (!refundTraceQueryRepository.existsRefund(refundId)) {
            throw new NotFoundException("refund not found");
        }

        String traceRequestId =
                refundTraceQueryRepository.findLatestNonNoOpRefundRequestId(refundId);

        if (traceRequestId == null || traceRequestId.isBlank()) {
            throw new NotFoundException("trace entry not found");
        }

        return new RefundTraceEntryResponseDTO(traceRequestId);
    }
}