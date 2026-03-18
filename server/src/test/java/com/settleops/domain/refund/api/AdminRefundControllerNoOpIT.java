package com.settleops.domain.refund.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AdminRefundControllerNoOpIT {
    //이미 처리된 환불을 다시 approve/reject 해도 상태가 바뀌지 않고 200 OK와 최소필드(status + decidedAt + requestId)를 반환하는지 검증하는 통합 테스트
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter MYSQL_DT6 =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    @BeforeEach
    void cleanup() {
        // 테스트 격리: refund 테이블만 정리
        // FK가 아직 OFF라는 전제(기획서 V99에서 FK ON).
        jdbcTemplate.update("delete from refund");
    }

    @Test
    @WithMockUser(username = "admin01", roles = "ADMIN")
    void approve_noOp200_whenAlreadyApproved_returnsStatusDecidedAtRequestId() throws Exception {
        String refundId = uuid();
        String paymentId = uuid();
        String merchantId = "MRC_0001"; // 32자 이하, 공백 없음(LOCKED)
        String buyerId = "BUY_0001";
        long amount = 1000L;

        LocalDateTime requestedAt = LocalDateTime.of(2026, 3, 5, 10, 0, 0, 0);
        LocalDateTime decidedAt = LocalDateTime.of(2026, 3, 5, 11, 0, 0, 0);

        insertRefund(refundId, paymentId, merchantId, buyerId, amount,
                "APPROVED", requestedAt, decidedAt);

        String requestId = uuid();

        mockMvc.perform(
                        patch("/api/admin/refunds/{refundId}/approve", refundId)
                                .header("X-Request-Id", requestId)
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        { "comment": "idempotent retry approve" }
                                        """)
                )
                .andExpect(status().isOk())
                // 최소 필드 계약(LOCKED)
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decidedAt").isNotEmpty())
                .andExpect(jsonPath("$.requestId").value(requestId));
    }

    @Test
    @WithMockUser(username = "admin01", roles = "ADMIN")
    void reject_noOp200_whenAlreadyRejected_returnsStatusDecidedAtRequestId() throws Exception {
        String refundId = uuid();
        String paymentId = uuid();
        String merchantId = "MRC_0001";
        String buyerId = "BUY_0001";
        long amount = 1000L;

        LocalDateTime requestedAt = LocalDateTime.of(2026, 3, 5, 10, 0, 0, 0);
        LocalDateTime decidedAt = LocalDateTime.of(2026, 3, 5, 11, 0, 0, 0);

        insertRefund(refundId, paymentId, merchantId, buyerId, amount,
                "REJECTED", requestedAt, decidedAt);

        String requestId = uuid();

        mockMvc.perform(
                        patch("/api/admin/refunds/{refundId}/reject", refundId)
                                .header("X-Request-Id", requestId)
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        { "comment": "idempotent retry reject" }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.decidedAt").isNotEmpty())
                .andExpect(jsonPath("$.requestId").value(requestId));
    }

    @Test
    @WithMockUser(username = "admin01", roles = "ADMIN")
    void approve_requiresComment_blank_returns400() throws Exception {
        String refundId = uuid();
        String paymentId = uuid();
        String merchantId = "MRC_0001";
        String buyerId = "BUY_0001";
        long amount = 1000L;

        LocalDateTime requestedAt = LocalDateTime.of(2026, 3, 5, 10, 0, 0, 0);
        LocalDateTime decidedAt = LocalDateTime.of(2026, 3, 5, 11, 0, 0, 0);

        insertRefund(refundId, paymentId, merchantId, buyerId, amount,
                "APPROVED", requestedAt, decidedAt);

        String requestId = uuid();

        mockMvc.perform(
                        patch("/api/admin/refunds/{refundId}/approve", refundId)
                                .header("X-Request-Id", requestId)
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        { "comment": "   " }
                                        """)
                )
                .andExpect(status().isBadRequest());
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
        // refund 테이블 DDL 기준 (스크린샷: merchant_id/buyer_id/status/requested_at/decided_at 등)
        // created_at/updated_at NOT NULL 이므로 같이 넣습니다.
        String now = LocalDateTime.now().format(MYSQL_DT6);

        jdbcTemplate.update("""
                insert into refund (
                    refund_id, payment_id, merchant_id, buyer_id,
                    amount, currency, status, reason_text,
                    requested_at, decided_at, created_at, updated_at
                )
                values (?, ?, ?, ?, ?, 'KRW', ?, 'test reason', ?, ?, ?, ?)
                """,
                refundId, paymentId, merchantId, buyerId,
                amount, status,
                requestedAt.format(MYSQL_DT6),
                decidedAt == null ? null : decidedAt.format(MYSQL_DT6),
                now, now
        );
    }

    private static String uuid() {
        return UUID.randomUUID().toString();
    }
}