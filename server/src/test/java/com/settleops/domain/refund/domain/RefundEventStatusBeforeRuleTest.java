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
}