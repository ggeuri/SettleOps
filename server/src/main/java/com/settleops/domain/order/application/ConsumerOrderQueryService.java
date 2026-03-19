package com.settleops.domain.order.application;

import com.settleops.domain.order.api.dto.ConsumerOrderDetailResponse;
import com.settleops.domain.order.infra.ConsumerOrderQueryRepository;
import com.settleops.global.error.ForbiddenException;
import com.settleops.global.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsumerOrderQueryService {

    private final ConsumerOrderQueryRepository consumerOrderQueryRepository;

    /**
     * Consumer 주문 상세 조회
     *
     * <p>IDOR 방지 정책:
     * <br>- 존재하지 않는 orderId는 먼저 404로 수렴한다.
     * <br>- 존재가 확인된 이후 owner(buyerId) 일치 여부를 검증한다.
     * <br>- 이를 통해 미존재 자원과 권한 불일치 자원의 응답 기준을 분리한다.
     * </p>
     */
    public ConsumerOrderDetailResponse getOrderDetail(String orderId, String loginConsumer) {
        ConsumerOrderDetailResponse detail = consumerOrderQueryRepository.findConsumerOrderDetail(orderId);

        if (detail == null) {
            throw new NotFoundException("order not found");
        }

        validateConsumerAccess(detail.buyerId(), loginConsumer);
        return detail;
    }

    private void validateConsumerAccess(String buyerId, String loginConsumer) {
        if (!buyerId.equals(loginConsumer)) {
            throw new ForbiddenException("다른 구매자의 주문은 조회할 수 없습니다.");
        }
    }
}