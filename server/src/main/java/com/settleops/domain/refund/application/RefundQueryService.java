package com.settleops.domain.refund.application;

import com.settleops.domain.refund.api.dto.RefundRowDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface RefundQueryService {

    Page<RefundRowDTO> myRefunds(
            String loginMerchantId,
            String status,
            LocalDate from,
            LocalDate to,
            String keyword,
            String sortKey,
            String sortDirection,
            Pageable pageable
    );
}