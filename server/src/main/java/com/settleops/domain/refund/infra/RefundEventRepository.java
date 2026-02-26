package com.settleops.domain.refund.infra;

import com.settleops.domain.refund.domain.RefundEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundEventRepository extends JpaRepository<RefundEvent, Long> {
    //jpa
}
