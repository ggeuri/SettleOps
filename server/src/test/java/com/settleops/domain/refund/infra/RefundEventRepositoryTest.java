package com.settleops.domain.refund.infra;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.settleops.domain.refund.domain.RefundEvent;
import com.settleops.domain.refund.domain.RefundEventType;
import com.settleops.domain.refund.domain.RefundStatus;
import com.settleops.global.audit.ActorType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test-db")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RefundEventRepositoryTest.QuerydslTestConfig.class)
class RefundEventRepositoryTest {

    //“refund_event는 insert-only 이벤트로, request_id와 occurredAt이 항상 채워져 저장된다”

    @Autowired
    RefundEventRepository refundEventRepository;

    //테스트 삽입 DB로 확인용
    //@org.springframework.test.annotation.Commit
    @Test
    void saveAndFlush하면_requestId_notNull_occurredAt_notNull() {
        RefundEvent ev = RefundEvent.builder()
                .refundId(UUID.randomUUID().toString())
                .eventType(RefundEventType.REFUND_REQUESTED)
                .statusBefore(null)
                .statusAfter(RefundStatus.REQUESTED)
                .requestId(UUID.randomUUID().toString())
                .actorType(ActorType.ADMIN)
                .actorId("admin1")
                .occurredAt(null)
                .build();

        RefundEvent saved = refundEventRepository.saveAndFlush(ev);

        assertThat(saved.getRefundEventId()).isNotNull();
        assertThat(saved.getRequestId()).isNotBlank();
        assertThat(saved.getOccurredAt()).isNotNull();
    }

    static class QuerydslTestConfig {
        @Bean
        JPAQueryFactory jpaQueryFactory(EntityManager em) {
            return new JPAQueryFactory(em);
        }
    }
}