package com.settleops.domain.order.api;

import com.settleops.domain.order.api.dto.ConsumerOrderDetailResponse;
import com.settleops.domain.order.application.ConsumerOrderQueryService;
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.auth.SessionAuthProvider;
import com.settleops.global.auth.resolver.LoginAdminArgumentResolver;
import com.settleops.global.auth.resolver.LoginConsumerArgumentResolver;
import com.settleops.global.auth.resolver.LoginMerchantArgumentResolver;
import com.settleops.global.error.ForbiddenException;
import com.settleops.global.error.GlobalExceptionHandler;
import com.settleops.global.error.NotFoundException;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConsumerOrderQueryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, LoginConsumerArgumentResolver.class})
class ConsumerOrderQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsumerOrderQueryService consumerOrderQueryService;

    @MockitoBean
    private SessionAuthProvider sessionAuthProvider;

    @MockitoBean
    private LoginAdminArgumentResolver loginAdminArgumentResolver;

    @MockitoBean
    private AuditLogger auditLogger;

    @Test
    @DisplayName("주문 상세 조회 성공 시 200을 반환한다")
    void getOrderDetail_success() throws Exception {
        String orderId = "11111111-1111-1111-1111-111111111111";
        String loginConsumerId = "buyer-1";

        ConsumerOrderDetailResponse response = new ConsumerOrderDetailResponse(
                orderId,
                "merchant-1",
                "buyer-1",
                "아이템",
                1000L,
                "CREATED",
                "44444444-4444-4444-4444-444444444444",
                "CAPTURED",
                LocalDateTime.of(2026, 3, 19, 12, 0, 0),
                LocalDateTime.of(2026, 3, 19, 12, 5, 0),
                List.of(
                        new ConsumerOrderDetailResponse.PaymentEventItem(
                                "PAYMENT_CREATED",
                                null,
                                "CREATED",
                                LocalDateTime.of(2026, 3, 19, 11, 59, 0)
                        ),
                        new ConsumerOrderDetailResponse.PaymentEventItem(
                                "PAYMENT_CAPTURED",
                                "CREATED",
                                "CAPTURED",
                                LocalDateTime.of(2026, 3, 19, 12, 0, 0)
                        ),
                        new ConsumerOrderDetailResponse.PaymentEventItem(
                                "PAYMENT_CONFIRMED",
                                null,
                                null,
                                LocalDateTime.of(2026, 3, 19, 12, 5, 0)
                        )
                )
        );

        BDDMockito.given(sessionAuthProvider.getRequiredBuyerId(ArgumentMatchers.any(HttpSession.class)))
                .willReturn(loginConsumerId);

        BDDMockito.given(consumerOrderQueryService.getOrderDetail(orderId, loginConsumerId))
                .willReturn(response);

        mockMvc.perform(get("/api/consumer/orders/{orderId}", orderId).sessionAttr("LOGIN_CONSUMER", "dummy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.merchantId").value("merchant-1"))
                .andExpect(jsonPath("$.buyerId").value("buyer-1"))
                .andExpect(jsonPath("$.itemName").value("아이템"))
                .andExpect(jsonPath("$.amount").value(1000))
                .andExpect(jsonPath("$.orderStatus").value("CREATED"))
                .andExpect(jsonPath("$.paymentId").value("44444444-4444-4444-4444-444444444444"))
                .andExpect(jsonPath("$.paymentStatus").value("CAPTURED"));
    }

    @Test
    @DisplayName("존재하지 않는 orderId면 404를 반환한다")
    void getOrderDetail_notFound_then404() throws Exception {
        String orderId = "22222222-2222-2222-2222-222222222222";
        String loginConsumerId = "buyer-1";

        BDDMockito.given(sessionAuthProvider.getRequiredBuyerId(ArgumentMatchers.any(HttpSession.class)))
                .willReturn(loginConsumerId);

        BDDMockito.given(consumerOrderQueryService.getOrderDetail(orderId, loginConsumerId))
                .willThrow(new NotFoundException("order not found"));

        mockMvc.perform(get("/api/consumer/orders/{orderId}", orderId).sessionAttr("LOGIN_CONSUMER", "dummy"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("order not found"));
    }

    @Test
    @DisplayName("다른 구매자의 주문이면 403을 반환한다")
    void getOrderDetail_forbidden_then403() throws Exception {
        String orderId = "33333333-3333-3333-3333-333333333333";
        String loginConsumerId = "buyer-2";

        BDDMockito.given(sessionAuthProvider.getRequiredBuyerId(ArgumentMatchers.any(HttpSession.class)))
                .willReturn(loginConsumerId);

        BDDMockito.given(consumerOrderQueryService.getOrderDetail(orderId, loginConsumerId))
                .willThrow(new ForbiddenException("다른 구매자의 주문은 조회할 수 없습니다."));

        mockMvc.perform(get("/api/consumer/orders/{orderId}", orderId).sessionAttr("LOGIN_CONSUMER", "dummy"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("다른 구매자의 주문은 조회할 수 없습니다."));
    }
}