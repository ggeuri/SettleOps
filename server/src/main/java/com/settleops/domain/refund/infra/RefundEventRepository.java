package com.settleops.domain.refund.infra;

import com.settleops.domain.refund.domain.RefundEvent;
import org.springframework.data.jpa.repository.JpaRepository;

// insert-only. save()만 사용, delete/update 금지.
public interface RefundEventRepository extends JpaRepository<RefundEvent, Long> {
    //jpa
}
