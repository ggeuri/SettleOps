package com.settleops.domain.payment.api;

import com.settleops.domain.payment.api.dto.PaymentDetailResponse;
import com.settleops.domain.payment.api.dto.RefundContextResponse;
import com.settleops.domain.payment.application.PaymentQueryService;
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.auth.SessionAuthProvider;
import com.settleops.global.auth.controller.MeController;
import com.settleops.global.auth.resolver.LoginMerchantArgumentResolver;
import com.settleops.global.error.ForbiddenException;
import com.settleops.global.error.GlobalExceptionHandler;
import com.settleops.global.error.NotFoundException;
import com.settleops.global.error.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PaymentQueryController 웹 계층 테스트
 *
 * <p>검증 범위</p>
 * <ul>
 *   <li>URL 매핑 및 JSON 응답 구조</li>
 *   <li>서비스 위임 결과의 HTTP 응답 변환</li>
 *   <li>NotFoundException 발생 시 404 응답 수렴</li>
 * </ul>
 *
 * <p>주의</p>
 * <ul>
 *   <li>보안/공통 필터 영향 없이 컨트롤러 계약만 검증하기 위해 addFilters=false 적용</li>
 *   <li>GlobalExceptionHandler 생성자 의존성 충족을 위해 AuditLogger를 mock bean으로 등록</li>
 * </ul>
 */
@WebMvcTest(PaymentQueryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, LoginMerchantArgumentResolver.class})
@ActiveProfiles("test")
class PaymentQueryControllerTest {

    private static final String PAYMENT_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String PAYMENT_ID_MISSING = "99999999-9999-9999-9999-999999999999";
    private static final String MERCHANT_ID = "MERCHANT_1";

    @Autowired
    private MockMvc mockMvc;

    /** 컨트롤러가 의존하는 조회 서비스 mock */
    @MockitoBean
    private PaymentQueryService paymentQueryService;

    /** GlobalExceptionHandler 생성자 주입용 mock */
    @MockitoBean
    private AuditLogger auditLogger;

    /** @LoginMerchant resolver 내부 의존 mock */
    @MockitoBean
    private SessionAuthProvider sessionAuthProvider;

    // =========================================================
    // U3. 결제 상세 조회
    // =========================================================

    @Nested
    class GetPaymentDetailTest {

        @Test
        @DisplayName("GET /api/payments/{paymentId} - 결제 상세 정상 조회")
        void getPaymentDetail_returns_ok() throws Exception {
            // given
            PaymentDetailResponse response = new PaymentDetailResponse(
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

            response.assignEvents(List.of(
                    new PaymentDetailResponse.PaymentEventItem(
                            "PAYMENT_CREATED",
                            "CREATED",
                            "CREATED",
                            LocalDateTime.of(2026, 3, 13, 9, 0, 0)
                    ),
                    new PaymentDetailResponse.PaymentEventItem(
                            "PAYMENT_CAPTURED",
                            "CREATED",
                            "CAPTURED",
                            LocalDateTime.of(2026, 3, 13, 9, 5, 0)
                    ),
                    new PaymentDetailResponse.PaymentEventItem(
                            "PAYMENT_CONFIRMED",
                            "CAPTURED",
                            "CAPTURED",
                            LocalDateTime.of(2026, 3, 13, 11, 0, 0)
                    )
            ));

            when(sessionAuthProvider.getRequiredMerchantId(any())).thenReturn(MERCHANT_ID);
            when(paymentQueryService.getPaymentDetail(eq(PAYMENT_ID), eq(MERCHANT_ID)))
                    .thenReturn(response);

            // when & then
            mockMvc.perform(get("/api/payments/{paymentId}", PAYMENT_ID)
                            .sessionAttr(MeController.SessionKeys.ROLE, "MERCHANT")
                            .sessionAttr(MeController.SessionKeys.MERCHANT_ID, MERCHANT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.paymentId").value(PAYMENT_ID))
                    .andExpect(jsonPath("$.orderId").value("11111111-1111-1111-1111-111111111111"))
                    .andExpect(jsonPath("$.merchantId").value(MERCHANT_ID))
                    .andExpect(jsonPath("$.buyerId").value("BUYER_1"))
                    .andExpect(jsonPath("$.status").value("CAPTURED"))
                    .andExpect(jsonPath("$.requestedAmount").value(350000))
                    .andExpect(jsonPath("$.capturedAmount").value(350000))
                    .andExpect(jsonPath("$.currency").value("KRW"))
                    .andExpect(jsonPath("$.confirmed").value(true))
                    .andExpect(jsonPath("$.events.length()").value(3))
                    .andExpect(jsonPath("$.events[0].eventType").value("PAYMENT_CREATED"))
                    .andExpect(jsonPath("$.events[1].eventType").value("PAYMENT_CAPTURED"))
                    .andExpect(jsonPath("$.events[2].eventType").value("PAYMENT_CONFIRMED"));

            verify(paymentQueryService).getPaymentDetail(eq(PAYMENT_ID), eq(MERCHANT_ID));
        }

        @Test
        @DisplayName("GET /api/payments/{paymentId} - 미존재 paymentId 조회 시 404")
        void getPaymentDetail_returns_not_found() throws Exception {
            // given
            when(sessionAuthProvider.getRequiredMerchantId(any())).thenReturn(MERCHANT_ID);
            when(paymentQueryService.getPaymentDetail(eq(PAYMENT_ID_MISSING), eq(MERCHANT_ID)))
                    .thenThrow(new NotFoundException("payment not found"));

            // when & then
            mockMvc.perform(get("/api/payments/{paymentId}", PAYMENT_ID_MISSING)
                            .sessionAttr(MeController.SessionKeys.ROLE, "MERCHANT")
                            .sessionAttr(MeController.SessionKeys.MERCHANT_ID, MERCHANT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.reason").doesNotExist())
                    .andExpect(jsonPath("$.message").value("payment not found"));

            verify(paymentQueryService).getPaymentDetail(eq(PAYMENT_ID_MISSING), eq(MERCHANT_ID));
        }

        @Test
        @DisplayName("GET /api/payments/{paymentId} - 세션 없으면 401")
        void getPaymentDetail_returns_unauthorized_when_session_missing() throws Exception {
            // given
            when(sessionAuthProvider.getRequiredMerchantId(any()))
                    .thenThrow(new UnauthorizedException("로그인이 필요합니다."));

            // when & then
            mockMvc.perform(get("/api/payments/{paymentId}", PAYMENT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.reason").doesNotExist())
                    .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
        }

        @Test
        @DisplayName("GET /api/payments/{paymentId} - 다른 merchant 소유면 403")
        void getPaymentDetail_returns_forbidden_when_merchant_mismatch() throws Exception {
            // given
            when(sessionAuthProvider.getRequiredMerchantId(any())).thenReturn(MERCHANT_ID);
            when(paymentQueryService.getPaymentDetail(eq(PAYMENT_ID), eq(MERCHANT_ID)))
                    .thenThrow(new ForbiddenException("다른 상점의 데이터는 조회할 수 없습니다."));

            // when & then
            mockMvc.perform(get("/api/payments/{paymentId}", PAYMENT_ID)
                            .sessionAttr(MeController.SessionKeys.ROLE, "MERCHANT")
                            .sessionAttr(MeController.SessionKeys.MERCHANT_ID, MERCHANT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                    .andExpect(jsonPath("$.reason").doesNotExist())
                    .andExpect(jsonPath("$.message").value("다른 상점의 데이터는 조회할 수 없습니다."));

            verify(paymentQueryService).getPaymentDetail(eq(PAYMENT_ID), eq(MERCHANT_ID));
        }
    }

    // =========================================================
    // refund-context 조회
    // =========================================================

    @Nested
    class GetRefundContextTest {

        @Test
        @DisplayName("GET /api/payments/{paymentId}/refund-context - 정상 조회")
        void getRefundContext_returns_ok() throws Exception {
            // given
            RefundContextResponse response = new RefundContextResponse(
                    PAYMENT_ID,
                    "CAPTURED",
                    500_000L,
                    400_000L,
                    "KRW",
                    MERCHANT_ID,
                    LocalDateTime.of(2026, 3, 13, 11, 3, 0)
            );

            when(sessionAuthProvider.getRequiredMerchantId(any())).thenReturn(MERCHANT_ID);
            when(paymentQueryService.getRefundContext(eq(PAYMENT_ID), eq(MERCHANT_ID)))
                    .thenReturn(response);

            // when & then
            mockMvc.perform(get("/api/payments/{paymentId}/refund-context", PAYMENT_ID)
                            .sessionAttr(MeController.SessionKeys.ROLE, "MERCHANT")
                            .sessionAttr(MeController.SessionKeys.MERCHANT_ID, MERCHANT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.paymentId").value(PAYMENT_ID))
                    .andExpect(jsonPath("$.status").value("CAPTURED"))
                    .andExpect(jsonPath("$.capturedAmount").value(500000))
                    .andExpect(jsonPath("$.refundableAmount").value(400000))
                    .andExpect(jsonPath("$.currency").value("KRW"))
                    .andExpect(jsonPath("$.merchantId").value(MERCHANT_ID));

            verify(paymentQueryService).getRefundContext(eq(PAYMENT_ID), eq(MERCHANT_ID));
        }

        @Test
        @DisplayName("GET /api/payments/{paymentId}/refund-context - 미존재 paymentId 조회 시 404")
        void getRefundContext_returns_not_found() throws Exception {
            // given
            when(sessionAuthProvider.getRequiredMerchantId(any())).thenReturn(MERCHANT_ID);
            when(paymentQueryService.getRefundContext(eq(PAYMENT_ID_MISSING), eq(MERCHANT_ID)))
                    .thenThrow(new NotFoundException("payment not found"));

            // when & then
            mockMvc.perform(get("/api/payments/{paymentId}/refund-context", PAYMENT_ID_MISSING)
                            .sessionAttr(MeController.SessionKeys.ROLE, "MERCHANT")
                            .sessionAttr(MeController.SessionKeys.MERCHANT_ID, MERCHANT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.reason").doesNotExist())
                    .andExpect(jsonPath("$.message").value("payment not found"));

            verify(paymentQueryService).getRefundContext(eq(PAYMENT_ID_MISSING), eq(MERCHANT_ID));
        }

        @Test
        @DisplayName("GET /api/payments/{paymentId}/refund-context - 세션 없으면 401")
        void getRefundContext_returns_unauthorized_when_session_missing() throws Exception {
            // given
            when(sessionAuthProvider.getRequiredMerchantId(any()))
                    .thenThrow(new UnauthorizedException("로그인이 필요합니다."));

            // when & then
            mockMvc.perform(get("/api/payments/{paymentId}/refund-context", PAYMENT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.reason").doesNotExist())
                    .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
        }

        @Test
        @DisplayName("GET /api/payments/{paymentId}/refund-context - 다른 merchant 소유면 403")
        void getRefundContext_returns_forbidden_when_merchant_mismatch() throws Exception {
            // given
            when(sessionAuthProvider.getRequiredMerchantId(any())).thenReturn(MERCHANT_ID);
            when(paymentQueryService.getRefundContext(eq(PAYMENT_ID), eq(MERCHANT_ID)))
                    .thenThrow(new ForbiddenException("다른 상점의 데이터는 조회할 수 없습니다."));

            // when & then
            mockMvc.perform(get("/api/payments/{paymentId}/refund-context", PAYMENT_ID)
                            .sessionAttr(MeController.SessionKeys.ROLE, "MERCHANT")
                            .sessionAttr(MeController.SessionKeys.MERCHANT_ID, MERCHANT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                    .andExpect(jsonPath("$.reason").doesNotExist())
                    .andExpect(jsonPath("$.message").value("다른 상점의 데이터는 조회할 수 없습니다."));

            verify(paymentQueryService).getRefundContext(eq(PAYMENT_ID), eq(MERCHANT_ID));
        }
    }
}