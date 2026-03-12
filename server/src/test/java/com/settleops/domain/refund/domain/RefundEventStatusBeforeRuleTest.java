package com.settleops.domain.refund.domain;

import com.settleops.global.audit.ActorType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefundEventStatusBeforeRuleTest {

    @Test
    void REQUESTED는_before_null_허용() {
        RefundEvent ev = RefundEventFactory.requested(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                ActorType.MERCHANT,
                "m1",
                null
        );

        assertThat(ev.getStatusBefore()).isNull();
        assertThat(ev.getStatusAfter()).isEqualTo(RefundStatus.REQUESTED);
    }

    @Test
    void APPROVED는_before가_REQUESTED가_아니면_실패() {
        assertThatThrownBy(() -> RefundEventFactory.approved(
                UUID.randomUUID().toString(),
                RefundStatus.REJECTED, // 일부러 틀림
                UUID.randomUUID().toString(),
                ActorType.ADMIN,
                "a1",
                null
        )).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void REJECTED는_before가_REQUESTED가_아니면_실패() {
        assertThatThrownBy(() -> RefundEventFactory.rejected(
                UUID.randomUUID().toString(),
                RefundStatus.APPROVED, // 일부러 틀림
                UUID.randomUUID().toString(),
                ActorType.ADMIN,
                "a1",
                null
        )).isInstanceOf(IllegalStateException.class);
    }
    @Test
    void RefundEvent에_Immutable_어노테이션이_있어야_한다() {
        // 왜 필요하냐면: @Immutable이 없으면 Hibernate가 UPDATE SQL을 방출할 수 있음
        // insert-only 계약을 코드 레벨에서 강제하는 테스트
        boolean hasImmutable = RefundEvent.class.isAnnotationPresent(
                org.hibernate.annotations.Immutable.class
        );
        assertThat(hasImmutable)
                .as("RefundEvent는 insert-only이므로 @Immutable 어노테이션이 반드시 있어야 한다")
                .isTrue();
    }

    @Test
    void APPROVED는_before_REQUESTED면_성공() {
        RefundEvent ev = RefundEventFactory.approved(
                UUID.randomUUID().toString(),
                RefundStatus.REQUESTED,
                UUID.randomUUID().toString(),
                ActorType.ADMIN,
                "a1",
                null
        );

        assertThat(ev.getStatusBefore()).isEqualTo(RefundStatus.REQUESTED);
        assertThat(ev.getStatusAfter()).isEqualTo(RefundStatus.APPROVED);
    }

    @Test
    void REJECTED는_before_REQUESTED면_성공() {
        RefundEvent ev = RefundEventFactory.rejected(
                UUID.randomUUID().toString(),
                RefundStatus.REQUESTED,
                UUID.randomUUID().toString(),
                ActorType.ADMIN,
                "a1",
                null
        );

        assertThat(ev.getStatusBefore()).isEqualTo(RefundStatus.REQUESTED);
        assertThat(ev.getStatusAfter()).isEqualTo(RefundStatus.REJECTED);
    }

}