package com.settleops.domain.refund.application;

import com.settleops.domain.refund.api.dto.AdminRefundListItemDTO;

import java.util.List;

public interface RefundQueryService {

    List<AdminRefundListItemDTO> myRefunds();

}
