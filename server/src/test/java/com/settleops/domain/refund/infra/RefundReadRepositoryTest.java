package com.settleops.domain.refund.infra;

import com.settleops.domain.refund.api.dto.AdminRefundListItemDTO;
import com.settleops.domain.refund.domain.RefundStatus;
import com.settleops.support.QuerydslTestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({RefundReadRepository.class, QuerydslTestConfig.class})
@ActiveProfiles("test-db")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RefundReadRepositoryTest {

    private static final DateTimeFormatter MYSQL_DT6 =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    @Autowired
    private RefundReadRepository refundReadRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("A6 환불 큐 조회는 offset/limit/count 기준으로 pagination 된다")
    void findAdminRefundQueue_appliesPagination() {
        // given
        LocalDateTime base = LocalDateTime.of(2026, 3, 22, 10, 0, 0);

        insertRefund(
                "refund-001", "payment-001", "merchant-1", "buyer-1", 1000L, "REQUESTED",
                base.minusMinutes(1), null
        );
        insertRefund(
                "refund-002", "payment-002", "merchant-1", "buyer-2", 2000L, "REQUESTED",
                base.minusMinutes(2), null
        );
        insertRefund(
                "refund-003", "payment-003", "merchant-1", "buyer-3", 3000L, "REQUESTED",
                base.minusMinutes(3), null
        );
        insertRefund(
                "refund-004", "payment-004", "merchant-1", "buyer-4", 4000L, "REQUESTED",
                base.minusMinutes(4), null
        );
        insertRefund(
                "refund-005", "payment-005", "merchant-1", "buyer-5", 5000L, "REQUESTED",
                base.minusMinutes(5), null
        );

        PageRequest pageable = PageRequest.of(1, 2); // 두 번째 페이지, 2개씩

        // when
        Page<AdminRefundListItemDTO> result = refundReadRepository.findAdminRefundQueue(
                RefundStatus.REQUESTED,
                null,
                null,
                pageable
        );

        // then
        // total count
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3);

        // limit
        assertThat(result.getContent()).hasSize(2);

        // offset + 정렬(requestedAt desc) 기준
        List<AdminRefundListItemDTO> content = result.getContent();
        assertThat(content.get(0).getRefundId()).isEqualTo("refund-003");
        assertThat(content.get(1).getRefundId()).isEqualTo("refund-004");
    }

    @Test
    @DisplayName("A6 환불 큐 조회는 status 조건과 pagination을 함께 적용한다")
    void findAdminRefundQueue_filtersByStatus_withPagination() {
        // given
        LocalDateTime base = LocalDateTime.of(2026, 3, 22, 11, 0, 0);

        insertRefund(
                "refund-a1", "payment-a1", "merchant-1", "buyer-1", 1000L, "REQUESTED",
                base.minusMinutes(1), null
        );
        insertRefund(
                "refund-a2", "payment-a2", "merchant-1", "buyer-2", 2000L, "REQUESTED",
                base.minusMinutes(2), null
        );
        insertRefund(
                "refund-a3", "payment-a3", "merchant-1", "buyer-3", 3000L, "REQUESTED",
                base.minusMinutes(3), null
        );
        insertRefund(
                "refund-b1", "payment-b1", "merchant-1", "buyer-4", 4000L, "APPROVED",
                base.minusMinutes(4), base.minusMinutes(3)
        );
        insertRefund(
                "refund-b2", "payment-b2", "merchant-1", "buyer-5", 5000L, "REJECTED",
                base.minusMinutes(5), base.minusMinutes(4)
        );

        PageRequest pageable = PageRequest.of(0, 2);

        // when
        Page<AdminRefundListItemDTO> result = refundReadRepository.findAdminRefundQueue(
                RefundStatus.REQUESTED,
                null,
                null,
                pageable
        );

        // then
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);

        assertThat(result.getContent())
                .extracting(AdminRefundListItemDTO::getStatus)
                .containsOnly(RefundStatus.REQUESTED);

        assertThat(result.getContent())
                .extracting(AdminRefundListItemDTO::getRefundId)
                .containsExactly("refund-a1", "refund-a2");
    }

    private void insertRefund(
            String refundId,
            String paymentId,
            String merchantId,
            String buyerId,
            long amount,
            String status,
            LocalDateTime requestedAt,
            LocalDateTime decidedAt
    ) {
        String now = LocalDateTime.now().format(MYSQL_DT6);

        jdbcTemplate.update("""
                insert into refund (
                    refund_id, payment_id, merchant_id, buyer_id,
                    amount, currency, status, reason_text,
                    requested_at, decided_at, created_at, updated_at
                )
                values (?, ?, ?, ?, ?, 'KRW', ?, 'test reason', ?, ?, ?, ?)
                """,
                refundId,
                paymentId,
                merchantId,
                buyerId,
                amount,
                status,
                requestedAt.format(MYSQL_DT6),
                decidedAt == null ? null : decidedAt.format(MYSQL_DT6),
                now,
                now
        );
    }

    @SuppressWarnings("unused")
    private static String uuid() {
        return UUID.randomUUID().toString();
    }
}