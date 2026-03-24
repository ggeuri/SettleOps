package com.settleops.domain.refund.infra;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test-db")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest
class RefundTraceQueryRepositoryMySqlIT {

    @Autowired
    private RefundTraceQueryRepository refundTraceQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter MYSQL_DT6 =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    @BeforeEach
    void cleanup() {
        jdbcTemplate.update("delete from audit_log");
        jdbcTemplate.update("delete from refund");
    }

    @Test
    @DisplayName("최신 refund audit가 no-op이면 이전 non-no-op requestId를 반환한다")
    void findLatestNonNoOpRefundRequestId_skipsLatestNoOpAudit() {
        String refundId = uuid();
        String paymentId = uuid();
        String merchantId = "MRC_0001";
        String buyerId = "BUY_0001";

        insertRefund(
                refundId,
                paymentId,
                merchantId,
                buyerId,
                1000L,
                "APPROVED",
                LocalDateTime.of(2026, 3, 24, 10, 0, 0),
                LocalDateTime.of(2026, 3, 24, 10, 10, 0)
        );

        insertAuditLog(
                "request-non-noop",
                LocalDateTime.of(2026, 3, 24, 10, 10, 0),
                "ADMIN",
                "admin01",
                "REFUND_APPROVED",
                "REFUND",
                refundId,
                "REQUESTED",
                "APPROVED",
                merchantId,
                """
                {"noOp":false,"comment":"approved","before":{"status":"REQUESTED"},"after":{"status":"APPROVED"}}
                """
        );

        insertAuditLog(
                "request-noop-latest",
                LocalDateTime.of(2026, 3, 24, 10, 20, 0),
                "ADMIN",
                "admin01",
                "REFUND_APPROVED",
                "REFUND",
                refundId,
                "APPROVED",
                "APPROVED",
                merchantId,
                """
                {"noOp":true,"noOpReason":"ALREADY_APPROVED","comment":"retry","before":{"status":"APPROVED"},"after":{"status":"APPROVED"}}
                """
        );

        String traceRequestId =
                refundTraceQueryRepository.findLatestNonNoOpRefundRequestId(refundId);

        assertThat(traceRequestId).isEqualTo("request-non-noop");
    }

    @Test
    @DisplayName("refund audit가 모두 no-op이면 null을 반환한다")
    void findLatestNonNoOpRefundRequestId_returnsNull_whenAllAuditsAreNoOp() {
        String refundId = uuid();
        String paymentId = uuid();
        String merchantId = "MRC_0001";
        String buyerId = "BUY_0001";

        insertRefund(
                refundId,
                paymentId,
                merchantId,
                buyerId,
                1000L,
                "APPROVED",
                LocalDateTime.of(2026, 3, 24, 10, 0, 0),
                LocalDateTime.of(2026, 3, 24, 10, 10, 0)
        );

        insertAuditLog(
                "request-noop-only",
                LocalDateTime.of(2026, 3, 24, 10, 20, 0),
                "ADMIN",
                "admin01",
                "REFUND_APPROVED",
                "REFUND",
                refundId,
                "APPROVED",
                "APPROVED",
                merchantId,
                """
                {"noOp":true,"noOpReason":"ALREADY_APPROVED","comment":"retry","before":{"status":"APPROVED"},"after":{"status":"APPROVED"}}
                """
        );

        String traceRequestId =
                refundTraceQueryRepository.findLatestNonNoOpRefundRequestId(refundId);

        assertThat(traceRequestId).isNull();
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

    private void insertAuditLog(
            String requestId,
            LocalDateTime occurredAt,
            String actorType,
            String actorId,
            String action,
            String entityType,
            String entityId,
            String statusBefore,
            String statusAfter,
            String merchantId,
            String metaJson
    ) {
        jdbcTemplate.update("""
                insert into audit_log (
                    request_id, occurred_at, actor_type, actor_id,
                    action, entity_type, entity_id,
                    status_before, status_after, merchant_id, meta_json
                )
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, cast(? as json))
                """,
                requestId,
                occurredAt.format(MYSQL_DT6),
                actorType,
                actorId,
                action,
                entityType,
                entityId,
                statusBefore,
                statusAfter,
                merchantId,
                metaJson
        );
    }

    private static String uuid() {
        return UUID.randomUUID().toString();
    }
}