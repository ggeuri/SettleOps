package com.settleops.domain.refund.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RefundEventOccurredAtTest {

    @Test
    void occurredAt_null이면_prePersist에서_now로_채워진다() {
        // given
        RefundEvent event = RefundEvent.builder()
                .refundId("r1")
                .eventType(RefundEventType.REFUND_REQUESTED)
                .statusBefore(null)
                .statusAfter(RefundStatus.REQUESTED)
                .requestId("req1")
                .actorType(com.settleops.global.audit.ActorType.ADMIN)
                .actorId("admin1")
                .occurredAt(null) // 핵심
                .build();

        // when
        event.prePersist(); // 같은 패키지라서 호출 가능(package-private)

        // then
        assertThat(event.getOccurredAt()).isNotNull();
        // (선택) 너무 과거/미래가 아니어야 한다 정도만
        assertThat(event.getOccurredAt()).isAfter(LocalDateTime.now().minusMinutes(1));
    }
}