package com.settleops.domain.refund.application;

import com.settleops.domain.refund.api.dto.RefundRowDTO;

import java.util.List;

public interface RefundQueryService {
    List<RefundRowDTO> myRefunds();
}