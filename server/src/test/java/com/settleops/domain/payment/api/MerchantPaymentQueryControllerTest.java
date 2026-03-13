package com.settleops.domain.payment.api;

import com.settleops.domain.payment.api.dto.MerchantPaymentListItemResponse;
import com.settleops.domain.payment.api.dto.MerchantPaymentSearchCondition;
import com.settleops.domain.payment.application.PaymentQueryService;
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.error.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MerchantPaymentQueryController 웹 계층 테스트
 *
 * <p>검증 범위</p>
 * <ul>
 *   <li>GET /api/merchants/{merchantId}/payments 매핑</li>
 *   <li>query param -> MerchantPaymentSearchCondition 바인딩</li>
 *   <li>서비스 위임 결과의 JSON 응답 변환</li>
 * </ul>
 *
 * <p>주의</p>
 * <ul>
 *   <li>보안/공통 필터 영향 없이 컨트롤러 계약만 검증하기 위해 addFilters=false 적용</li>
 *   <li>GlobalExceptionHandler 생성자 의존성 충족을 위해 AuditLogger를 mock bean으로 등록</li>
 * </ul>
 */
@WebMvcTest(MerchantPaymentQueryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class MerchantPaymentQueryControllerTest {

    private static final String MERCHANT_ID = "MERCHANT_1";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentQueryService paymentQueryService;

    @MockitoBean
    private AuditLogger auditLogger;

    // =========================================================
    // U2. Merchant 결제 목록 조회
    // =========================================================

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
                            200_000L,
                            200_000L,
                            "KRW",
                            "BUYER_2",
                            LocalDateTime.of(2026, 3, 12, 10, 0, 0),
                            false,
                            null
                    )
            );

            when(paymentQueryService.getMerchantPayments(eq(MERCHANT_ID), any(MerchantPaymentSearchCondition.class)))
                    .thenReturn(response);

            // when & then
            mockMvc.perform(get("/api/merchants/{merchantId}/payments", MERCHANT_ID)
                            .param("status", "CAPTURED")
                            .param("confirmed", "CONFIRMED")
                            .param("from", "2026-03-10")
                            .param("to", "2026-03-13")
                            .param("keyword", "아이폰"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/json"))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].paymentId").value("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                    .andExpect(jsonPath("$[0].orderId").value("11111111-1111-1111-1111-111111111111"))
                    .andExpect(jsonPath("$[0].status").value("CAPTURED"))
                    .andExpect(jsonPath("$[0].requestedAmount").value(100000))
                    .andExpect(jsonPath("$[0].capturedAmount").value(100000))
                    .andExpect(jsonPath("$[0].currency").value("KRW"))
                    .andExpect(jsonPath("$[0].buyerId").value("BUYER_1"))
                    .andExpect(jsonPath("$[0].confirmed").value(true))
                    .andExpect(jsonPath("$[1].paymentId").value("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"))
                    .andExpect(jsonPath("$[1].confirmed").value(false));

            ArgumentCaptor<MerchantPaymentSearchCondition> captor =
                    ArgumentCaptor.forClass(MerchantPaymentSearchCondition.class);

            verify(paymentQueryService).getMerchantPayments(eq(MERCHANT_ID), captor.capture());

            MerchantPaymentSearchCondition condition = captor.getValue();
            assertThat(readField(condition, "status")).isEqualTo("CAPTURED");
            assertThat(readField(condition, "confirmed")).isEqualTo("CONFIRMED");
            assertThat(readField(condition, "keyword")).isEqualTo("아이폰");
            assertThat(readField(condition, "from")).isEqualTo(LocalDate.of(2026, 3, 10));
            assertThat(readField(condition, "to")).isEqualTo(LocalDate.of(2026, 3, 13));
        }

        @Test
        @DisplayName("GET /api/merchants/{merchantId}/payments - 결과 없으면 빈 배열 반환")
        void getMerchantPayments_returns_empty_list() throws Exception {
            // given
            when(paymentQueryService.getMerchantPayments(eq(MERCHANT_ID), any(MerchantPaymentSearchCondition.class)))
                    .thenReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/merchants/{merchantId}/payments", MERCHANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/json"))
                    .andExpect(jsonPath("$.length()").value(0));

            verify(paymentQueryService).getMerchantPayments(eq(MERCHANT_ID), any(MerchantPaymentSearchCondition.class));
        }
    }

    @Test
    @DisplayName("GET /api/merchants/{merchantId}/payments - query param 없이 호출 가능")
    void getMerchantPayments_without_query_params_returns_ok() throws Exception {
        // given
        when(paymentQueryService.getMerchantPayments(eq(MERCHANT_ID), any(MerchantPaymentSearchCondition.class)))
                .thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/merchants/{merchantId}/payments", MERCHANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.length()").value(0));

        ArgumentCaptor<MerchantPaymentSearchCondition> captor =
                ArgumentCaptor.forClass(MerchantPaymentSearchCondition.class);

        verify(paymentQueryService).getMerchantPayments(eq(MERCHANT_ID), captor.capture());

        MerchantPaymentSearchCondition condition = captor.getValue();
        assertThat(readField(condition, "status")).isNull();
        assertThat(readField(condition, "confirmed")).isNull();
        assertThat(readField(condition, "from")).isNull();
        assertThat(readField(condition, "to")).isNull();
        assertThat(readField(condition, "keyword")).isNull();
    }

    @Test
    @DisplayName("GET /api/merchants/{merchantId}/payments - status 단독 바인딩")
    void getMerchantPayments_binds_status_only() throws Exception {
        // given
        when(paymentQueryService.getMerchantPayments(eq(MERCHANT_ID), any(MerchantPaymentSearchCondition.class)))
                .thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/merchants/{merchantId}/payments", MERCHANT_ID)
                        .param("status", "CAPTURED"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.length()").value(0));

        ArgumentCaptor<MerchantPaymentSearchCondition> captor =
                ArgumentCaptor.forClass(MerchantPaymentSearchCondition.class);

        verify(paymentQueryService).getMerchantPayments(eq(MERCHANT_ID), captor.capture());

        MerchantPaymentSearchCondition condition = captor.getValue();
        assertThat(readField(condition, "status")).isEqualTo("CAPTURED");
        assertThat(readField(condition, "confirmed")).isNull();
        assertThat(readField(condition, "from")).isNull();
        assertThat(readField(condition, "to")).isNull();
        assertThat(readField(condition, "keyword")).isNull();
    }

    @Test
    @DisplayName("GET /api/merchants/{merchantId}/payments - from/to 날짜 바인딩")
    void getMerchantPayments_binds_from_and_to_as_local_date() throws Exception {
        // given
        when(paymentQueryService.getMerchantPayments(eq(MERCHANT_ID), any(MerchantPaymentSearchCondition.class)))
                .thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/merchants/{merchantId}/payments", MERCHANT_ID)
                        .param("from", "2026-03-10")
                        .param("to", "2026-03-13"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.length()").value(0));

        ArgumentCaptor<MerchantPaymentSearchCondition> captor =
                ArgumentCaptor.forClass(MerchantPaymentSearchCondition.class);

        verify(paymentQueryService).getMerchantPayments(eq(MERCHANT_ID), captor.capture());

        MerchantPaymentSearchCondition condition = captor.getValue();
        assertThat(readField(condition, "from")).isEqualTo(LocalDate.of(2026, 3, 10));
        assertThat(readField(condition, "to")).isEqualTo(LocalDate.of(2026, 3, 13));
    }

    /**
     * 현재 DTO를 변경하지 않고 query param 바인딩 결과를 확인하기 위해 reflection으로 필드값을 읽는다.
     */
    private Object readField(Object target, String fieldName) {
        return ReflectionTestUtils.getField(target, fieldName);
    }
}