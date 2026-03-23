package com.settleops.domain.payment.api;

import com.settleops.domain.payment.api.dto.MerchantPaymentListItemResponse;
import com.settleops.domain.payment.api.dto.MerchantPaymentSearchCondition;
import com.settleops.domain.payment.application.PaymentQueryService;
import com.settleops.domain.payment.domain.PaymentStatus;
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.auth.SessionAuthProvider;
import com.settleops.global.auth.controller.MeController;
import com.settleops.global.auth.resolver.LoginMerchantArgumentResolver;
import com.settleops.global.error.ForbiddenException;
import com.settleops.global.error.GlobalExceptionHandler;
import com.settleops.global.error.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MerchantPaymentQueryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, LoginMerchantArgumentResolver.class})
@ActiveProfiles("test")
class MerchantPaymentQueryControllerTest {

    private static final String MERCHANT_ID = "MERCHANT_1";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentQueryService paymentQueryService;

    @MockitoBean
    private AuditLogger auditLogger;

    @MockitoBean
    private SessionAuthProvider sessionAuthProvider;

    @Nested
    class GetMerchantPaymentsTest {

        @Test
        @DisplayName("GET /api/merchants/{merchantId}/payments - 목록 정상 조회")
        void getMerchantPayments_returns_ok() throws Exception {
            // given
            List<MerchantPaymentListItemResponse> response = List.of(
                    new MerchantPaymentListItemResponse(
                            "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                            "11111111-1111-1111-1111-111111111111",
                            "CAPTURED",
                            "에어팟 프로",
                            100_000L,
                            100_000L,
                            "KRW",
                            "BUYER_1",
                            LocalDateTime.of(2026, 3, 13, 10, 0, 0),
                            true,
                            LocalDateTime.of(2026, 3, 13, 11, 0, 0)
                    ),
                    new MerchantPaymentListItemResponse(
                            "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                            "22222222-2222-2222-2222-222222222222",
                            "CAPTURED",
                            "에어팟 맥스",
                            200_000L,
                            200_000L,
                            "KRW",
                            "BUYER_2",
                            LocalDateTime.of(2026, 3, 12, 10, 0, 0),
                            false,
                            null
                    )
            );

            when(sessionAuthProvider.getCurrentMerchantId()).thenReturn(MERCHANT_ID);
            when(paymentQueryService.getMerchantPayments(
                    eq(MERCHANT_ID),
                    eq(MERCHANT_ID),
                    any(MerchantPaymentSearchCondition.class))
            ).thenReturn(new PageImpl<>(
                    response,
                    PageRequest.of(0, 20),
                    response.size()
            ));

            // when & then
            mockMvc.perform(get("/api/merchants/{merchantId}/payments", MERCHANT_ID)
                            .sessionAttr(MeController.SessionKeys.ROLE, "MERCHANT")
                            .sessionAttr(MeController.SessionKeys.MERCHANT_ID, MERCHANT_ID)
                            .param("status", "CAPTURED")
                            .param("from", "2026-03-10")
                            .param("to", "2026-03-13")
                            .param("keyword", "아이폰"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.items.length()").value(2))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.hasNext").value(false))
                    .andExpect(jsonPath("$.hasPrevious").value(false))
                    .andExpect(jsonPath("$.items[0].paymentId").value("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                    .andExpect(jsonPath("$.items[0].orderId").value("11111111-1111-1111-1111-111111111111"))
                    .andExpect(jsonPath("$.items[0].status").value("CAPTURED"))
                    .andExpect(jsonPath("$.items[0].requestedAmount").value(100000))
                    .andExpect(jsonPath("$.items[0].capturedAmount").value(100000))
                    .andExpect(jsonPath("$.items[0].currency").value("KRW"))
                    .andExpect(jsonPath("$.items[0].buyerId").value("BUYER_1"))
                    .andExpect(jsonPath("$.items[0].confirmed").value(true))
                    .andExpect(jsonPath("$.items[1].paymentId").value("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"))
                    .andExpect(jsonPath("$.items[1].confirmed").value(false));

            ArgumentCaptor<MerchantPaymentSearchCondition> captor =
                    ArgumentCaptor.forClass(MerchantPaymentSearchCondition.class);

            verify(paymentQueryService).getMerchantPayments(
                    eq(MERCHANT_ID),
                    eq(MERCHANT_ID),
                    captor.capture()
            );

            MerchantPaymentSearchCondition condition = captor.getValue();
            assertThat(readField(condition, "status")).isEqualTo(PaymentStatus.CAPTURED);
            assertThat(readField(condition, "keyword")).isEqualTo("아이폰");
            assertThat(readField(condition, "from")).isEqualTo(LocalDate.of(2026, 3, 10));
            assertThat(readField(condition, "to")).isEqualTo(LocalDate.of(2026, 3, 13));
            assertThat(readField(condition, "page")).isNull();
            assertThat(readField(condition, "size")).isNull();
        }

        @Test
        @DisplayName("GET /api/merchants/{merchantId}/payments - 결과 없으면 빈 items page 반환")
        void getMerchantPayments_returns_empty_page() throws Exception {
            // given
            when(sessionAuthProvider.getCurrentMerchantId()).thenReturn(MERCHANT_ID);
            when(paymentQueryService.getMerchantPayments(
                    eq(MERCHANT_ID),
                    eq(MERCHANT_ID),
                    any(MerchantPaymentSearchCondition.class))
            ).thenReturn(new PageImpl<>(
                    List.of(),
                    PageRequest.of(0, 20),
                    0
            ));

            // when & then
            mockMvc.perform(get("/api/merchants/{merchantId}/payments", MERCHANT_ID)
                            .sessionAttr(MeController.SessionKeys.ROLE, "MERCHANT")
                            .sessionAttr(MeController.SessionKeys.MERCHANT_ID, MERCHANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.items.length()").value(0))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.totalPages").value(0))
                    .andExpect(jsonPath("$.hasNext").value(false))
                    .andExpect(jsonPath("$.hasPrevious").value(false));

            verify(paymentQueryService).getMerchantPayments(
                    eq(MERCHANT_ID),
                    eq(MERCHANT_ID),
                    any(MerchantPaymentSearchCondition.class)
            );
        }

        @Test
        @DisplayName("GET /api/merchants/{merchantId}/payments - 잘못된 status면 400")
        void getMerchantPayments_returns_bad_request_when_status_invalid() throws Exception {
            // given
            when(sessionAuthProvider.getCurrentMerchantId()).thenReturn(MERCHANT_ID);

            // when & then
            mockMvc.perform(get("/api/merchants/{merchantId}/payments", MERCHANT_ID)
                            .sessionAttr(MeController.SessionKeys.ROLE, "MERCHANT")
                            .sessionAttr(MeController.SessionKeys.MERCHANT_ID, MERCHANT_ID)
                            .param("status", "INVALID_STATUS"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.reason").doesNotExist());
        }

        @Test
        @DisplayName("GET /api/merchants/{merchantId}/payments - merchant 불일치면 403")
        void getMerchantPayments_returns_forbidden_when_merchant_mismatch() throws Exception {
            // given
            when(sessionAuthProvider.getCurrentMerchantId())
                    .thenThrow(new ForbiddenException("접근 권한이 없습니다."));

            // when & then
            mockMvc.perform(get("/api/merchants/{merchantId}/payments", "OTHER_MERCHANT")
                            .sessionAttr(MeController.SessionKeys.ROLE, "MERCHANT")
                            .sessionAttr(MeController.SessionKeys.MERCHANT_ID, MERCHANT_ID))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                    .andExpect(jsonPath("$.reason").doesNotExist())
                    .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));

            verifyNoInteractions(paymentQueryService);
        }

        @Test
        @DisplayName("GET /api/merchants/{merchantId}/payments - query param 없이 호출 가능")
        void getMerchantPayments_without_query_params_returns_ok() throws Exception {
            // given
            when(sessionAuthProvider.getCurrentMerchantId()).thenReturn(MERCHANT_ID);
            when(paymentQueryService.getMerchantPayments(
                    eq(MERCHANT_ID),
                    eq(MERCHANT_ID),
                    any(MerchantPaymentSearchCondition.class))
            ).thenReturn(new PageImpl<>(
                    List.of(),
                    PageRequest.of(0, 20),
                    0
            ));

            // when & then
            mockMvc.perform(get("/api/merchants/{merchantId}/payments", MERCHANT_ID)
                            .sessionAttr(MeController.SessionKeys.ROLE, "MERCHANT")
                            .sessionAttr(MeController.SessionKeys.MERCHANT_ID, MERCHANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.items.length()").value(0))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.totalPages").value(0))
                    .andExpect(jsonPath("$.hasNext").value(false))
                    .andExpect(jsonPath("$.hasPrevious").value(false));

            ArgumentCaptor<MerchantPaymentSearchCondition> captor =
                    ArgumentCaptor.forClass(MerchantPaymentSearchCondition.class);

            verify(paymentQueryService).getMerchantPayments(
                    eq(MERCHANT_ID),
                    eq(MERCHANT_ID),
                    captor.capture()
            );

            MerchantPaymentSearchCondition condition = captor.getValue();
            assertThat(readField(condition, "status")).isNull();
            assertThat(readField(condition, "from")).isNull();
            assertThat(readField(condition, "to")).isNull();
            assertThat(readField(condition, "keyword")).isNull();
            assertThat(readField(condition, "page")).isNull();
            assertThat(readField(condition, "size")).isNull();
        }

        @Test
        @DisplayName("GET /api/merchants/{merchantId}/payments - status 단독 바인딩")
        void getMerchantPayments_binds_status_only() throws Exception {
            // given
            when(sessionAuthProvider.getCurrentMerchantId()).thenReturn(MERCHANT_ID);
            when(paymentQueryService.getMerchantPayments(
                    eq(MERCHANT_ID),
                    eq(MERCHANT_ID),
                    any(MerchantPaymentSearchCondition.class))
            ).thenReturn(new PageImpl<>(
                    List.of(),
                    PageRequest.of(0, 20),
                    0
            ));

            // when & then
            mockMvc.perform(get("/api/merchants/{merchantId}/payments", MERCHANT_ID)
                            .sessionAttr(MeController.SessionKeys.ROLE, "MERCHANT")
                            .sessionAttr(MeController.SessionKeys.MERCHANT_ID, MERCHANT_ID)
                            .param("status", "CAPTURED"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.items.length()").value(0))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.totalPages").value(0))
                    .andExpect(jsonPath("$.hasNext").value(false))
                    .andExpect(jsonPath("$.hasPrevious").value(false));

            ArgumentCaptor<MerchantPaymentSearchCondition> captor =
                    ArgumentCaptor.forClass(MerchantPaymentSearchCondition.class);

            verify(paymentQueryService).getMerchantPayments(
                    eq(MERCHANT_ID),
                    eq(MERCHANT_ID),
                    captor.capture()
            );

            MerchantPaymentSearchCondition condition = captor.getValue();
            assertThat(readField(condition, "status")).isEqualTo(PaymentStatus.CAPTURED);
            assertThat(readField(condition, "from")).isNull();
            assertThat(readField(condition, "to")).isNull();
            assertThat(readField(condition, "keyword")).isNull();
            assertThat(readField(condition, "page")).isNull();
            assertThat(readField(condition, "size")).isNull();
        }

        @Test
        @DisplayName("GET /api/merchants/{merchantId}/payments - from/to 날짜 바인딩")
        void getMerchantPayments_binds_from_and_to_as_local_date() throws Exception {
            // given
            when(sessionAuthProvider.getCurrentMerchantId()).thenReturn(MERCHANT_ID);
            when(paymentQueryService.getMerchantPayments(
                    eq(MERCHANT_ID),
                    eq(MERCHANT_ID),
                    any(MerchantPaymentSearchCondition.class))
            ).thenReturn(new PageImpl<>(
                    List.of(),
                    PageRequest.of(0, 20),
                    0
            ));

            // when & then
            mockMvc.perform(get("/api/merchants/{merchantId}/payments", MERCHANT_ID)
                            .sessionAttr(MeController.SessionKeys.ROLE, "MERCHANT")
                            .sessionAttr(MeController.SessionKeys.MERCHANT_ID, MERCHANT_ID)
                            .param("from", "2026-03-10")
                            .param("to", "2026-03-13"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.items.length()").value(0))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.totalPages").value(0))
                    .andExpect(jsonPath("$.hasNext").value(false))
                    .andExpect(jsonPath("$.hasPrevious").value(false));

            ArgumentCaptor<MerchantPaymentSearchCondition> captor =
                    ArgumentCaptor.forClass(MerchantPaymentSearchCondition.class);

            verify(paymentQueryService).getMerchantPayments(
                    eq(MERCHANT_ID),
                    eq(MERCHANT_ID),
                    captor.capture()
            );

            MerchantPaymentSearchCondition condition = captor.getValue();
            assertThat(readField(condition, "from")).isEqualTo(LocalDate.of(2026, 3, 10));
            assertThat(readField(condition, "to")).isEqualTo(LocalDate.of(2026, 3, 13));
            assertThat(readField(condition, "page")).isNull();
            assertThat(readField(condition, "size")).isNull();
        }

        @Test
        @DisplayName("GET /api/merchants/{merchantId}/payments - page/size 바인딩")
        void getMerchantPayments_binds_page_and_size() throws Exception {
            // given
            when(sessionAuthProvider.getCurrentMerchantId()).thenReturn(MERCHANT_ID);
            when(paymentQueryService.getMerchantPayments(
                    eq(MERCHANT_ID),
                    eq(MERCHANT_ID),
                    any(MerchantPaymentSearchCondition.class))
            ).thenReturn(new PageImpl<>(
                    List.of(),
                    PageRequest.of(2, 10),
                    25
            ));

            // when & then
            mockMvc.perform(get("/api/merchants/{merchantId}/payments", MERCHANT_ID)
                            .sessionAttr(MeController.SessionKeys.ROLE, "MERCHANT")
                            .sessionAttr(MeController.SessionKeys.MERCHANT_ID, MERCHANT_ID)
                            .param("page", "2")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.items.length()").value(0))
                    .andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.totalElements").value(25))
                    .andExpect(jsonPath("$.totalPages").value(3))
                    .andExpect(jsonPath("$.hasNext").value(false))
                    .andExpect(jsonPath("$.hasPrevious").value(true));

            ArgumentCaptor<MerchantPaymentSearchCondition> captor =
                    ArgumentCaptor.forClass(MerchantPaymentSearchCondition.class);

            verify(paymentQueryService).getMerchantPayments(
                    eq(MERCHANT_ID),
                    eq(MERCHANT_ID),
                    captor.capture()
            );

            MerchantPaymentSearchCondition condition = captor.getValue();
            assertThat(readField(condition, "page")).isEqualTo(2);
            assertThat(readField(condition, "size")).isEqualTo(10);
        }

        @Test
        @DisplayName("GET /api/merchants/{merchantId}/payments - 세션 없으면 401")
        void getMerchantPayments_returns_unauthorized_when_session_missing() throws Exception {
            // given
            when(sessionAuthProvider.getCurrentMerchantId())
                    .thenThrow(new UnauthorizedException("로그인이 필요합니다."));

            // when & then
            mockMvc.perform(get("/api/merchants/{merchantId}/payments", MERCHANT_ID))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.reason").doesNotExist())
                    .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));

            verifyNoInteractions(paymentQueryService);
        }
    }

    private Object readField(Object target, String fieldName) {
        return ReflectionTestUtils.getField(target, fieldName);
    }
}