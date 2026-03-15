package com.settleops.domain.payment.application;

import com.settleops.domain.payment.api.dto.MerchantPaymentListItemResponse;
import com.settleops.domain.payment.api.dto.MerchantPaymentSearchCondition;
import com.settleops.domain.payment.api.dto.PaymentDetailResponse;
import com.settleops.domain.payment.api.dto.RefundContextResponse;
import com.settleops.domain.payment.infra.PaymentQueryRepository;
import com.settleops.global.error.ForbiddenException;
import com.settleops.global.error.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentQueryServiceTest {

    private static final String MERCHANT_ID = "MERCHANT_1";
    private static final String OTHER_MERCHANT_ID = "MERCHANT_2"; // ✅ 권한 불일치 테스트용
    private static final String PAYMENT_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String PAYMENT_ID_MISSING = "99999999-9999-9999-9999-999999999999";

    @Mock
    private PaymentQueryRepository paymentQueryRepository;

    @InjectMocks
    private PaymentQueryService paymentQueryService;

    @Test
    void getMerchantPayments_should_throw_forbidden_when_merchant_mismatch() {
        // given
        MerchantPaymentSearchCondition condition = new MerchantPaymentSearchCondition();

        // when & then
        assertThatThrownBy(() ->
                paymentQueryService.getMerchantPayments(MERCHANT_ID, OTHER_MERCHANT_ID, condition)
        ).isInstanceOf(ForbiddenException.class)
                .hasMessage("다른 상점의 데이터는 조회할 수 없습니다.");
    }

    // =========================================================
    // U2. Merchant 결제 목록 조회
    // =========================================================

    @Test
    @DisplayName("U2_merchant 결제 목록 조회를 repository에 위임")
    void getMerchantPayments_delegates_to_repository() {
        // given
        MerchantPaymentSearchCondition condition = new MerchantPaymentSearchCondition();

        List<MerchantPaymentListItemResponse> expected = List.of(
                new MerchantPaymentListItemResponse(
                        PAYMENT_ID,
                        "11111111-1111-1111-1111-111111111111",
                        "CAPTURED",
                        100_000L,
                        100_000L,
                        "KRW",
                        "BUYER_1",
                        LocalDateTime.of(2026, 3, 13, 10, 0, 0),
                        true,
                        LocalDateTime.of(2026, 3, 13, 11, 0, 0)
                )
        );

        when(paymentQueryRepository.searchMerchantPayments(MERCHANT_ID, condition))
                .thenReturn(expected);

        // when
        List<MerchantPaymentListItemResponse> result =
                paymentQueryService.getMerchantPayments(MERCHANT_ID, MERCHANT_ID, condition);

        // then
        assertThat(result).isEqualTo(expected);
        verify(paymentQueryRepository).searchMerchantPayments(MERCHANT_ID, condition);
    }

    // =========================================================
    // U3. 결제 상세 조회
    // =========================================================

    @Nested
    class GetPaymentDetailTest {

        @Test
        @DisplayName("U3_결제 상세 조회를 repository에 위임")
        void getPaymentDetail_returns_detail() {
            // given
            PaymentDetailResponse expected = new PaymentDetailResponse(
                    PAYMENT_ID,
                    "11111111-1111-1111-1111-111111111111",
                    MERCHANT_ID,
                    "BUYER_1",
                    "CAPTURED",
                    350_000L,
                    350_000L,
                    "KRW",
                    LocalDateTime.of(2026, 3, 13, 9, 0, 0),
                    LocalDateTime.of(2026, 3, 13, 9, 5, 0),
                    true,
                    LocalDateTime.of(2026, 3, 13, 11, 0, 0)
            );

            when(paymentQueryRepository.findPaymentDetail(PAYMENT_ID))
                    .thenReturn(expected);

            // when
            PaymentDetailResponse result = paymentQueryService.getPaymentDetail(PAYMENT_ID, MERCHANT_ID);

            // then
            assertThat(result).isEqualTo(expected);
            verify(paymentQueryRepository).findPaymentDetail(PAYMENT_ID);
        }

        @Test
        @DisplayName("U3_미존재 paymentId 조회 시 NotFoundException 발생")
        void getPaymentDetail_throws_not_found_when_repository_returns_null() {
            // given
            when(paymentQueryRepository.findPaymentDetail(PAYMENT_ID_MISSING))
                    .thenReturn(null);

            // when & then
            assertThatThrownBy(() -> paymentQueryService.getPaymentDetail(PAYMENT_ID_MISSING, MERCHANT_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("payment not found");

            verify(paymentQueryRepository).findPaymentDetail(PAYMENT_ID_MISSING);
        }

        @Test
        @DisplayName("U3_다른 상점 paymentId 조회 시 ForbiddenException 발생")
        void getPaymentDetail_throws_forbidden_when_merchant_mismatch() {
            // given
            PaymentDetailResponse expected = new PaymentDetailResponse(
                    PAYMENT_ID,
                    "11111111-1111-1111-1111-111111111111",
                    MERCHANT_ID,
                    "BUYER_1",
                    "CAPTURED",
                    350_000L,
                    350_000L,
                    "KRW",
                    LocalDateTime.of(2026, 3, 13, 9, 0, 0),
                    LocalDateTime.of(2026, 3, 13, 9, 5, 0),
                    true,
                    LocalDateTime.of(2026, 3, 13, 11, 0, 0)
            );

            when(paymentQueryRepository.findPaymentDetail(PAYMENT_ID))
                    .thenReturn(expected);

            // when & then
            assertThatThrownBy(() -> paymentQueryService.getPaymentDetail(PAYMENT_ID, OTHER_MERCHANT_ID))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("다른 상점의 데이터는 조회할 수 없습니다.");

            verify(paymentQueryRepository).findPaymentDetail(PAYMENT_ID);
        }
    }

    // =========================================================
    // Refund Context 조회
    // =========================================================

    @Nested
    class GetRefundContextTest {

        @Test
        @DisplayName("refund-context 조회를 repository에 위임")
        void getRefundContext_returns_context() {
            // given
            RefundContextResponse expected = new RefundContextResponse(
                    PAYMENT_ID,
                    "CAPTURED",
                    500_000L,
                    400_000L,
                    "KRW",
                    MERCHANT_ID,
                    LocalDateTime.of(2026, 3, 13, 11, 3, 0)
            );

            when(paymentQueryRepository.findRefundContext(PAYMENT_ID))
                    .thenReturn(expected);

            // when
            RefundContextResponse result = paymentQueryService.getRefundContext(PAYMENT_ID, MERCHANT_ID);

            // then
            assertThat(result).isEqualTo(expected);
            verify(paymentQueryRepository).findRefundContext(PAYMENT_ID);
        }

        @Test
        @DisplayName("refund-context 미존재 paymentId 조회 시 NotFoundException 발생")
        void getRefundContext_throws_not_found_when_repository_returns_null() {
            // given
            when(paymentQueryRepository.findRefundContext(PAYMENT_ID_MISSING))
                    .thenReturn(null);

            // when & then
            assertThatThrownBy(() -> paymentQueryService.getRefundContext(PAYMENT_ID_MISSING, MERCHANT_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("payment not found");

            verify(paymentQueryRepository).findRefundContext(PAYMENT_ID_MISSING);
        }

        @Test
        @DisplayName("refund-context 다른 상점 paymentId 조회 시 ForbiddenException 발생")
        void getRefundContext_throws_forbidden_when_merchant_mismatch() {
            // given
            RefundContextResponse expected = new RefundContextResponse(
                    PAYMENT_ID,
                    "CAPTURED",
                    500_000L,
                    400_000L,
                    "KRW",
                    MERCHANT_ID,
                    LocalDateTime.of(2026, 3, 13, 11, 3, 0)
            );

            when(paymentQueryRepository.findRefundContext(PAYMENT_ID))
                    .thenReturn(expected);

            // when & then
            assertThatThrownBy(() -> paymentQueryService.getRefundContext(PAYMENT_ID, OTHER_MERCHANT_ID))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("다른 상점의 데이터는 조회할 수 없습니다.");

            verify(paymentQueryRepository).findRefundContext(PAYMENT_ID);
        }
    }
}