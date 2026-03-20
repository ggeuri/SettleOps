package com.settleops.domain.payment.api;

import com.settleops.domain.payment.api.dto.ConfirmResponseDTO;
import com.settleops.domain.payment.application.ConfirmService;
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.auth.SessionAuthProvider;
import com.settleops.global.auth.controller.MeController;
import com.settleops.global.auth.resolver.LoginAdminArgumentResolver;
import com.settleops.global.auth.resolver.LoginConsumerArgumentResolver;
import com.settleops.global.auth.resolver.LoginMerchantArgumentResolver;
import com.settleops.global.enums.ReasonCode;
import com.settleops.global.error.ConflictException;
import com.settleops.global.error.ForbiddenException;
import com.settleops.global.error.GlobalExceptionHandler;
import com.settleops.global.web.RequestIdResolver;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConsumerPaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class ConfirmControllerTest {

    private static final String PAYMENT_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String ORDER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String BUYER_ID = "BUYER_1";
    private static final String REQUEST_ID = "90000000-0000-0000-0000-000000000001";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConfirmService confirmService;

    @MockitoBean
    private RequestIdResolver requestIdResolver;

    @MockitoBean
    private SessionAuthProvider sessionAuthProvider;

    @MockitoBean
    private LoginAdminArgumentResolver loginAdminArgumentResolver;

    @MockitoBean
    private LoginMerchantArgumentResolver loginMerchantArgumentResolver;

    @MockitoBean
    private LoginConsumerArgumentResolver loginConsumerArgumentResolver;

    @MockitoBean
    private AuditLogger auditLogger;

    @Nested
    class ConfirmTest {

        @Test
        @DisplayName("POST /api/consumer/payments/{paymentId}/confirm - 정상 확정")
        void confirm_returns_ok() throws Exception {
            // given
            ConfirmResponseDTO response = new ConfirmResponseDTO(
                    PAYMENT_ID,
                    ORDER_ID,
                    "CAPTURED",
                    LocalDateTime.of(2026, 3, 13, 11, 0, 0)
            );

            when(requestIdResolver.resolve(any())).thenReturn(REQUEST_ID);
            when(confirmService.confirm(eq(PAYMENT_ID), eq(BUYER_ID), eq(REQUEST_ID)))
                    .thenReturn(response);

            // when & then
            mockMvc.perform(post("/api/consumer/payments/{paymentId}/confirm", PAYMENT_ID)
                            .sessionAttr(MeController.SessionKeys.BUYER_ID, BUYER_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.paymentId").value(PAYMENT_ID))
                    .andExpect(jsonPath("$.orderId").value(ORDER_ID))
                    .andExpect(jsonPath("$.status").value("CAPTURED"))
                    .andExpect(jsonPath("$.confirmedAt").value("2026-03-13T11:00:00"));

            verify(confirmService).confirm(eq(PAYMENT_ID), eq(BUYER_ID), eq(REQUEST_ID));
        }

        @Test
        @DisplayName("POST /api/consumer/payments/{paymentId}/confirm - 세션 없으면 401")
        void confirm_returns_unauthorized_when_session_missing() throws Exception {
            mockMvc.perform(post("/api/consumer/payments/{paymentId}/confirm", PAYMENT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.reason").doesNotExist())
                    .andExpect(jsonPath("$.message").value("인증이 필요합니다."));

            verifyNoInteractions(confirmService);
        }

        @Test
        @DisplayName("POST /api/consumer/payments/{paymentId}/confirm - buyerId가 비어있으면 401")
        void confirm_returns_unauthorized_when_buyer_id_blank() throws Exception {
            mockMvc.perform(post("/api/consumer/payments/{paymentId}/confirm", PAYMENT_ID)
                            .sessionAttr(MeController.SessionKeys.BUYER_ID, " ")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.reason").doesNotExist())
                    .andExpect(jsonPath("$.message").value("인증이 필요합니다."));

            verifyNoInteractions(confirmService);
        }

        @Test
        @DisplayName("POST /api/consumer/payments/{paymentId}/confirm - buyer 불일치면 403")
        void confirm_returns_forbidden_when_buyer_mismatch() throws Exception {
            // given
            when(requestIdResolver.resolve(any())).thenReturn(REQUEST_ID);
            when(confirmService.confirm(eq(PAYMENT_ID), eq(BUYER_ID), eq(REQUEST_ID)))
                    .thenThrow(new ForbiddenException("구매자 정보가 일치하지 않습니다."));

            // when & then
            mockMvc.perform(post("/api/consumer/payments/{paymentId}/confirm", PAYMENT_ID)
                            .sessionAttr(MeController.SessionKeys.BUYER_ID, BUYER_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                    .andExpect(jsonPath("$.reason").doesNotExist())
                    .andExpect(jsonPath("$.message").value("구매자 정보가 일치하지 않습니다."));
        }

        @Test
        @DisplayName("POST /api/consumer/payments/{paymentId}/confirm - CAPTURED 상태가 아니면 409")
        void confirm_returns_conflict_when_payment_not_captured() throws Exception {
            // given
            when(requestIdResolver.resolve(any())).thenReturn(REQUEST_ID);
            when(confirmService.confirm(eq(PAYMENT_ID), eq(BUYER_ID), eq(REQUEST_ID)))
                    .thenThrow(new ConflictException(
                            ReasonCode.PAYMENT_NOT_CAPTURED,
                            "결제 상태가 CAPTURED가 아닙니다."
                    ));

            // when & then
            mockMvc.perform(post("/api/consumer/payments/{paymentId}/confirm", PAYMENT_ID)
                            .sessionAttr(MeController.SessionKeys.BUYER_ID, BUYER_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("RULE_VIOLATION"))
                    .andExpect(jsonPath("$.reason").value("PAYMENT_NOT_CAPTURED"));
        }
    }
}