package com.settleops.domain.payment.infra;

import com.settleops.domain.payment.api.dto.*;
import com.settleops.domain.payment.domain.PaymentStatus;
import com.settleops.support.QuerydslTestConfig;
import com.settleops.domain.payment.api.dto.ConfirmedFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PaymentQueryRepository 조회 테스트
 *
 * <p>검증 범위</p>
 * <ul>
 *   <li>U2 Merchant 결제 목록 조회</li>
 *   <li>U3 결제 상세 조회</li>
 *   <li>refund-context 조회</li>
 * </ul>
 *
 * <p>검증 목적</p>
 * <ul>
 *   <li>QueryDSL projection 정합 검증</li>
 *   <li>이벤트 기반 파생값(capturedAt, confirmedAt) 검증</li>
 *   <li>검색 조건(status/confirmed/기간/keyword) 동작 검증</li>
 *   <li>미존재 조회 및 빈 결과 처리 검증</li>
 * </ul>
 */
@DataJpaTest
@Import({PaymentQueryRepository.class, QuerydslTestConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class PaymentQueryRepositoryTest {

    private static final String ORDER_ID_1 = "11111111-1111-1111-1111-111111111111";
    private static final String ORDER_ID_2 = "22222222-2222-2222-2222-222222222222";
    private static final String ORDER_ID_3 = "33333333-3333-3333-3333-333333333333";
    private static final String ORDER_ID_DETAIL = "44444444-4444-4444-4444-444444444444";
    private static final String ORDER_ID_REFUND = "55555555-5555-5555-5555-555555555555";

    private static final String PAYMENT_ID_1 = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String PAYMENT_ID_2 = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final String PAYMENT_ID_3 = "cccccccc-cccc-cccc-cccc-cccccccccccc";
    private static final String PAYMENT_ID_DETAIL = "dddddddd-dddd-dddd-dddd-dddddddddddd";
    private static final String PAYMENT_ID_REFUND = "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee";
    private static final String PAYMENT_ID_MISSING = "99999999-9999-9999-9999-999999999999";

    private static final String REFUND_ID_1 = "ffffffff-ffff-ffff-ffff-ffffffffffff";

    private static final String REQUEST_ID_1 = "90000000-0000-0000-0000-000000000001";
    private static final String REQUEST_ID_2 = "90000000-0000-0000-0000-000000000002";
    private static final String REQUEST_ID_3 = "90000000-0000-0000-0000-000000000003";
    private static final String REQUEST_ID_4 = "90000000-0000-0000-0000-000000000004";
    private static final String REQUEST_ID_5 = "90000000-0000-0000-0000-000000000005";
    private static final String REQUEST_ID_6 = "90000000-0000-0000-0000-000000000006";
    private static final String REQUEST_ID_7 = "90000000-0000-0000-0000-000000000007";
    private static final String REQUEST_ID_8 = "90000000-0000-0000-0000-000000000008";
    private static final String REQUEST_ID_DETAIL_1 = "90000000-0000-0000-0000-000000000011";
    private static final String REQUEST_ID_DETAIL_2 = "90000000-0000-0000-0000-000000000012";
    private static final String REQUEST_ID_DETAIL_3 = "90000000-0000-0000-0000-000000000013";
    private static final String REQUEST_ID_REFUND_1 = "90000000-0000-0000-0000-000000000021";
    private static final String REQUEST_ID_REFUND_2 = "90000000-0000-0000-0000-000000000022";

    @Autowired
    private PaymentQueryRepository paymentQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // =========================================================
    // U2. Merchant 결제 목록 조회
    // =========================================================

    @Test
    @DisplayName("U2_merchantId/status/confirmed/keyword 조건과 파생값 조회")
    void searchMerchantPayments_filters_and_derives_fields() {
        // given
        LocalDateTime base = LocalDateTime.of(2026, 3, 13, 10, 0, 0);

        insertOrder(ORDER_ID_1, "MERCHANT_1", "BUYER_1", "아이폰 15", 100_000L, "PAID", base.minusDays(1), base.minusDays(1));
        insertOrder(ORDER_ID_2, "MERCHANT_1", "BUYER_2", "갤럭시 S25", 200_000L, "PAID", base.minusDays(2), base.minusDays(2));
        insertOrder(ORDER_ID_3, "MERCHANT_2", "BUYER_3", "맥북 프로", 300_000L, "PAID", base.minusDays(3), base.minusDays(3));

        insertPayment(PAYMENT_ID_1, ORDER_ID_1, "MERCHANT_1", "BUYER_1", "KRW", 100_000L, 100_000L, "CAPTURED", base.minusDays(1), base.minusDays(1));
        insertPayment(PAYMENT_ID_2, ORDER_ID_2, "MERCHANT_1", "BUYER_2", "KRW", 200_000L, 200_000L, "CAPTURED", base.minusDays(2), base.minusDays(2));
        insertPayment(PAYMENT_ID_3, ORDER_ID_3, "MERCHANT_2", "BUYER_3", "KRW", 300_000L, 300_000L, "CAPTURED", base.minusDays(3), base.minusDays(3));

        insertPaymentEvent(PAYMENT_ID_1, "PAYMENT_CREATED", "CREATED", "CREATED", REQUEST_ID_1, base.minusDays(1).minusMinutes(10));
        insertPaymentEvent(PAYMENT_ID_1, "PAYMENT_CAPTURED", "CREATED", "CAPTURED", REQUEST_ID_2, base.minusDays(1));
        insertPaymentEvent(PAYMENT_ID_1, "PAYMENT_CONFIRMED", "CAPTURED", "CAPTURED", REQUEST_ID_3, base.minusDays(1).plusHours(1));

        insertPaymentEvent(PAYMENT_ID_2, "PAYMENT_CREATED", "CREATED", "CREATED", REQUEST_ID_4, base.minusDays(2).minusMinutes(10));
        insertPaymentEvent(PAYMENT_ID_2, "PAYMENT_CAPTURED", "CREATED", "CAPTURED", REQUEST_ID_5, base.minusDays(2));

        insertPaymentEvent(PAYMENT_ID_3, "PAYMENT_CREATED", "CREATED", "CREATED", REQUEST_ID_6, base.minusDays(3).minusMinutes(10));
        insertPaymentEvent(PAYMENT_ID_3, "PAYMENT_CAPTURED", "CREATED", "CAPTURED", REQUEST_ID_7, base.minusDays(3));
        insertPaymentEvent(PAYMENT_ID_3, "PAYMENT_CONFIRMED", "CAPTURED", "CAPTURED", REQUEST_ID_8, base.minusDays(3).plusHours(2));

        MerchantPaymentSearchCondition condition = createSearchCondition(
                PaymentStatus.CAPTURED,
                ConfirmedFilter.CONFIRMED,
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 13),
                "아이폰"
        );

        // when
        List<MerchantPaymentListItemResponse> result =
                paymentQueryRepository.searchMerchantPayments("MERCHANT_1", condition);

        // then
        assertThat(result).hasSize(1);

        MerchantPaymentListItemResponse item = result.get(0);
        assertThat(item.getPaymentId().trim()).isEqualTo(PAYMENT_ID_1);
        assertThat(item.getOrderId().trim()).isEqualTo(ORDER_ID_1);
        assertThat(item.getStatus()).isEqualTo("CAPTURED");
        assertThat(item.getRequestedAmount()).isEqualTo(100_000L);
        assertThat(item.getCapturedAmount()).isEqualTo(100_000L);
        assertThat(item.getCurrency()).isEqualTo("KRW");
        assertThat(item.getBuyerId()).isEqualTo("BUYER_1");
        assertThat(item.isConfirmed()).isTrue();
        assertThat(item.getCapturedAt()).isEqualTo(base.minusDays(1));
        assertThat(item.getConfirmedAt()).isEqualTo(base.minusDays(1).plusHours(1));
    }

    @Test
    @DisplayName("U2_keyword가 paymentId에 대해 부분일치 검색된다")
    void searchMerchantPayments_filters_by_payment_id_keyword() {
        // given
        LocalDateTime base = LocalDateTime.of(2026, 3, 13, 10, 0, 0);

        insertOrder(ORDER_ID_1, "MERCHANT_1", "BUYER_1", "아이폰 15", 100_000L, "PAID", base, base);
        insertOrder(ORDER_ID_2, "MERCHANT_1", "BUYER_2", "갤럭시 S25", 200_000L, "PAID", base.minusDays(1), base.minusDays(1));

        insertPayment(PAYMENT_ID_1, ORDER_ID_1, "MERCHANT_1", "BUYER_1", "KRW", 100_000L, 100_000L, "CAPTURED", base, base);
        insertPayment(PAYMENT_ID_2, ORDER_ID_2, "MERCHANT_1", "BUYER_2", "KRW", 200_000L, 200_000L, "CAPTURED", base.minusDays(1), base.minusDays(1));

        insertPaymentEvent(PAYMENT_ID_1, "PAYMENT_CAPTURED", "CREATED", "CAPTURED", REQUEST_ID_1, base);
        insertPaymentEvent(PAYMENT_ID_2, "PAYMENT_CAPTURED", "CREATED", "CAPTURED", REQUEST_ID_2, base.minusDays(1));

        MerchantPaymentSearchCondition condition = createSearchCondition(null, null, null, null, "aaaaaaaa");

        // when
        List<MerchantPaymentListItemResponse> result =
                paymentQueryRepository.searchMerchantPayments("MERCHANT_1", condition);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPaymentId().trim()).isEqualTo(PAYMENT_ID_1);
    }

    @Test
    @DisplayName("U2_keyword가 orderId에 대해 부분일치 검색된다")
    void searchMerchantPayments_filters_by_order_id_keyword() {
        // given
        LocalDateTime base = LocalDateTime.of(2026, 3, 13, 10, 0, 0);

        insertOrder(ORDER_ID_1, "MERCHANT_1", "BUYER_1", "아이폰 15", 100_000L, "PAID", base, base);
        insertOrder(ORDER_ID_2, "MERCHANT_1", "BUYER_2", "갤럭시 S25", 200_000L, "PAID", base.minusDays(1), base.minusDays(1));

        insertPayment(PAYMENT_ID_1, ORDER_ID_1, "MERCHANT_1", "BUYER_1", "KRW", 100_000L, 100_000L, "CAPTURED", base, base);
        insertPayment(PAYMENT_ID_2, ORDER_ID_2, "MERCHANT_1", "BUYER_2", "KRW", 200_000L, 200_000L, "CAPTURED", base.minusDays(1), base.minusDays(1));

        insertPaymentEvent(PAYMENT_ID_1, "PAYMENT_CAPTURED", "CREATED", "CAPTURED", REQUEST_ID_1, base);
        insertPaymentEvent(PAYMENT_ID_2, "PAYMENT_CAPTURED", "CREATED", "CAPTURED", REQUEST_ID_2, base.minusDays(1));

        MerchantPaymentSearchCondition condition = createSearchCondition(null, null, null, null, "22222222");

        // when
        List<MerchantPaymentListItemResponse> result =
                paymentQueryRepository.searchMerchantPayments("MERCHANT_1", condition);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderId().trim()).isEqualTo(ORDER_ID_2);
    }

    @Test
    @DisplayName("U2_keyword가 itemName에 대해 부분일치 검색된다")
    void searchMerchantPayments_filters_by_item_name_keyword() {
        // given
        LocalDateTime base = LocalDateTime.of(2026, 3, 13, 10, 0, 0);

        insertOrder(ORDER_ID_1, "MERCHANT_1", "BUYER_1", "아이폰 15 프로", 100_000L, "PAID", base, base);
        insertOrder(ORDER_ID_2, "MERCHANT_1", "BUYER_2", "갤럭시 S25", 200_000L, "PAID", base.minusDays(1), base.minusDays(1));

        insertPayment(PAYMENT_ID_1, ORDER_ID_1, "MERCHANT_1", "BUYER_1", "KRW", 100_000L, 100_000L, "CAPTURED", base, base);
        insertPayment(PAYMENT_ID_2, ORDER_ID_2, "MERCHANT_1", "BUYER_2", "KRW", 200_000L, 200_000L, "CAPTURED", base.minusDays(1), base.minusDays(1));

        insertPaymentEvent(PAYMENT_ID_1, "PAYMENT_CAPTURED", "CREATED", "CAPTURED", REQUEST_ID_1, base);
        insertPaymentEvent(PAYMENT_ID_2, "PAYMENT_CAPTURED", "CREATED", "CAPTURED", REQUEST_ID_2, base.minusDays(1));

        MerchantPaymentSearchCondition condition = createSearchCondition(null, null, null, null, "아이폰");

        // when
        List<MerchantPaymentListItemResponse> result =
                paymentQueryRepository.searchMerchantPayments("MERCHANT_1", condition);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPaymentId().trim()).isEqualTo(PAYMENT_ID_1);
    }

    // =========================================================
    // U3. 결제 상세 조회
    // =========================================================

    @Test
    @DisplayName("U3_payment 상세 조회 시 이벤트 타임라인 오름차순 반환")
    void findPaymentDetail_returns_detail_and_events_sorted() {
        // given
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 13, 9, 0, 0);
        LocalDateTime capturedAt = createdAt.plusMinutes(5);
        LocalDateTime confirmedAt = createdAt.plusHours(2);

        insertOrder(ORDER_ID_DETAIL, "MERCHANT_1", "BUYER_1", "닌텐도 스위치", 350_000L, "PAID", createdAt, createdAt);
        insertPayment(PAYMENT_ID_DETAIL, ORDER_ID_DETAIL, "MERCHANT_1", "BUYER_1", "KRW", 350_000L, 350_000L, "CAPTURED", createdAt, createdAt);

        insertPaymentEvent(PAYMENT_ID_DETAIL, "PAYMENT_CREATED", "CREATED", "CREATED", REQUEST_ID_DETAIL_1, createdAt);
        insertPaymentEvent(PAYMENT_ID_DETAIL, "PAYMENT_CAPTURED", "CREATED", "CAPTURED", REQUEST_ID_DETAIL_2, capturedAt);
        insertPaymentEvent(PAYMENT_ID_DETAIL, "PAYMENT_CONFIRMED", "CAPTURED", "CAPTURED", REQUEST_ID_DETAIL_3, confirmedAt);

        // when
        PaymentDetailResponse result = paymentQueryRepository.findPaymentDetail(PAYMENT_ID_DETAIL);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPaymentId().trim()).isEqualTo(PAYMENT_ID_DETAIL);
        assertThat(result.getOrderId().trim()).isEqualTo(ORDER_ID_DETAIL);
        assertThat(result.getMerchantId()).isEqualTo("MERCHANT_1");
        assertThat(result.getBuyerId()).isEqualTo("BUYER_1");
        assertThat(result.getStatus()).isEqualTo("CAPTURED");
        assertThat(result.getRequestedAmount()).isEqualTo(350_000L);
        assertThat(result.getCapturedAmount()).isEqualTo(350_000L);
        assertThat(result.getCurrency()).isEqualTo("KRW");
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        assertThat(result.getCapturedAt()).isEqualTo(capturedAt);
        assertThat(result.isConfirmed()).isTrue();
        assertThat(result.getConfirmedAt()).isEqualTo(confirmedAt);

        assertThat(result.getEvents()).hasSize(3);
        assertThat(result.getEvents().get(0).getEventType()).isEqualTo("PAYMENT_CREATED");
        assertThat(result.getEvents().get(0).getOccurredAt()).isEqualTo(createdAt);

        assertThat(result.getEvents().get(1).getEventType()).isEqualTo("PAYMENT_CAPTURED");
        assertThat(result.getEvents().get(1).getOccurredAt()).isEqualTo(capturedAt);

        assertThat(result.getEvents().get(2).getEventType()).isEqualTo("PAYMENT_CONFIRMED");
        assertThat(result.getEvents().get(2).getOccurredAt()).isEqualTo(confirmedAt);
    }

    @Test
    @DisplayName("U3_이벤트가 없으면 빈 리스트를 반환한다")
    void findPaymentDetail_returns_empty_events_when_no_event_exists() {
        // given
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 13, 9, 0, 0);

        insertOrder(ORDER_ID_DETAIL, "MERCHANT_1", "BUYER_1", "닌텐도 스위치", 350_000L, "PAID", createdAt, createdAt);
        insertPayment(PAYMENT_ID_DETAIL, ORDER_ID_DETAIL, "MERCHANT_1", "BUYER_1", "KRW", 350_000L, 350_000L, "CAPTURED", createdAt, createdAt);

        // when
        PaymentDetailResponse result = paymentQueryRepository.findPaymentDetail(PAYMENT_ID_DETAIL);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getEvents()).isNotNull();
        assertThat(result.getEvents()).isEmpty();
    }

    // =========================================================
    // refund-context 조회
    // =========================================================

    @Test
    @DisplayName("refund-context는 APPROVED 환불 1건 기준 refundableAmount 계산")
    void findRefundContext_calculates_refundable_amount() {
        // given
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 13, 11, 0, 0);
        LocalDateTime capturedAt = createdAt.plusMinutes(3);

        insertOrder(ORDER_ID_REFUND, "MERCHANT_1", "BUYER_1", "에어팟 프로", 500_000L, "PAID", createdAt, createdAt);
        insertPayment(PAYMENT_ID_REFUND, ORDER_ID_REFUND, "MERCHANT_1", "BUYER_1", "KRW", 500_000L, 500_000L, "CAPTURED", createdAt, createdAt);

        insertPaymentEvent(PAYMENT_ID_REFUND, "PAYMENT_CREATED", "CREATED", "CREATED", REQUEST_ID_REFUND_1, createdAt);
        insertPaymentEvent(PAYMENT_ID_REFUND, "PAYMENT_CAPTURED", "CREATED", "CAPTURED", REQUEST_ID_REFUND_2, capturedAt);

        insertRefund(
                REFUND_ID_1,
                PAYMENT_ID_REFUND,
                "MERCHANT_1",
                "BUYER_1",
                100_000L,
                "KRW",
                "APPROVED",
                "사이즈 문제",
                createdAt.plusHours(1),
                createdAt.plusHours(2),
                createdAt.plusHours(1),
                createdAt.plusHours(2)
        );

        // when
        RefundContextResponse result = paymentQueryRepository.findRefundContext(PAYMENT_ID_REFUND);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getRefundableAmount()).isEqualTo(400_000L);
        assertThat(result.getCapturedAt()).isEqualTo(capturedAt);
        assertThat(result.getPaymentId().trim()).isEqualTo(PAYMENT_ID_REFUND);
        assertThat(result.getStatus()).isEqualTo("CAPTURED");
        assertThat(result.getCapturedAmount()).isEqualTo(500_000L);
        assertThat(result.getCurrency()).isEqualTo("KRW");
        assertThat(result.getMerchantId()).isEqualTo("MERCHANT_1");
    }

    // =========================================================
    // Not Found / Empty Result
    // =========================================================

    @Test
    @DisplayName("미존재 paymentId 상세 조회 시 null 반환")
    void findPaymentDetail_returns_null_when_payment_not_found() {
        // when
        PaymentDetailResponse result = paymentQueryRepository.findPaymentDetail(PAYMENT_ID_MISSING);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("미존재 paymentId refund-context 조회 시 null 반환")
    void findRefundContext_returns_null_when_payment_not_found() {
        // when
        RefundContextResponse result = paymentQueryRepository.findRefundContext(PAYMENT_ID_MISSING);

        // then
        assertThat(result).isNull();
    }

    // =========================================================
    // SearchCondition Helper
    // =========================================================

    /**
     * 테스트 전용 검색 조건 생성 helper
     * - 운영 DTO 변경 없이 필요한 필드만 세팅
     * - QueryDSL 조건 테스트에서 반복 생성 코드 제거 목적
     */
    private MerchantPaymentSearchCondition createSearchCondition(
            PaymentStatus status,
            ConfirmedFilter confirmed,
            LocalDate from,
            LocalDate to,
            String keyword
    ) {
        MerchantPaymentSearchCondition condition = new MerchantPaymentSearchCondition();
        condition.setStatus(status);
        condition.setConfirmed(confirmed);
        condition.setFrom(from);
        condition.setTo(to);
        condition.setKeyword(keyword);
        return condition;
    }

    // =========================================================
    // Fixture Setup Helpers
    // =========================================================

    /**
     * orders 테스트 fixture 적재
     * - merchant/buyer/item/amount 기반 조회 조건 검증용
     */
    private void insertOrder(
            String orderId,
            String merchantId,
            String buyerId,
            String itemName,
            long amount,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        jdbcTemplate.update(
                """
                insert into orders (
                    order_id, merchant_id, buyer_id, item_name, amount, currency, status, created_at, updated_at
                ) values (?, ?, ?, ?, ?, 'KRW', ?, ?, ?)
                """,
                orderId,
                merchantId,
                buyerId,
                itemName,
                amount,
                status,
                Timestamp.valueOf(createdAt),
                Timestamp.valueOf(updatedAt)
        );
    }

    /**
     * payment 테스트 fixture 적재
     * - payment/order/merchant/buyer 기반 목록/상세 조회 검증용
     */
    private void insertPayment(
            String paymentId,
            String orderId,
            String merchantId,
            String buyerId,
            String currency,
            long requestedAmount,
            long capturedAmount,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        jdbcTemplate.update(
                """
                insert into payment (
                    payment_id, order_id, merchant_id, buyer_id, currency,
                    requested_amount, captured_amount, status, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                paymentId,
                orderId,
                merchantId,
                buyerId,
                currency,
                requestedAmount,
                capturedAmount,
                status,
                Timestamp.valueOf(createdAt),
                Timestamp.valueOf(updatedAt)
        );
    }

    /**
     * payment_event 테스트 fixture 적재
     * - capturedAt/confirmedAt 파생값 및 이벤트 타임라인 검증용
     */
    private void insertPaymentEvent(
            String paymentId,
            String eventType,
            String statusBefore,
            String statusAfter,
            String requestId,
            LocalDateTime occurredAt
    ) {
        jdbcTemplate.update(
                """
                insert into payment_event (
                    payment_id, event_type, status_before, status_after, request_id, occurred_at
                ) values (?, ?, ?, ?, ?, ?)
                """,
                paymentId,
                eventType,
                statusBefore,
                statusAfter,
                requestId,
                Timestamp.valueOf(occurredAt)
        );
    }

    /**
     * refund 테스트 fixture 적재
     * - refundableAmount 계산 검증용
     */
    private void insertRefund(
            String refundId,
            String paymentId,
            String merchantId,
            String buyerId,
            long amount,
            String currency,
            String status,
            String reasonText,
            LocalDateTime requestedAt,
            LocalDateTime decidedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        jdbcTemplate.update(
                """
                insert into refund (
                    refund_id, payment_id, merchant_id, buyer_id, amount, currency,
                    status, reason_text, requested_at, decided_at, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                refundId,
                paymentId,
                merchantId,
                buyerId,
                amount,
                currency,
                status,
                reasonText,
                Timestamp.valueOf(requestedAt),
                decidedAt == null ? null : Timestamp.valueOf(decidedAt),
                Timestamp.valueOf(createdAt),
                Timestamp.valueOf(updatedAt)
        );
    }
}