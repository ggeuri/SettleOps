package com.settleops.domain.order.api;

import com.settleops.domain.order.application.ConsumerOrderFacade;
import com.settleops.domain.order.domain.Orders;
import com.settleops.global.audit.AuditLogger;
import com.settleops.global.auth.SessionAuthProvider;
import com.settleops.global.auth.resolver.LoginAdminArgumentResolver;
import com.settleops.global.web.RequestIdResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConsumerOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class ConsumerOrderControllerTest {

    @MockitoBean
    private AuditLogger auditLogger;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsumerOrderFacade consumerOrderFacade;

    @MockitoBean
    private SessionAuthProvider sessionAuthProvider;

    @MockitoBean
    private LoginAdminArgumentResolver loginAdminArgumentResolver;

    @Test
    @DisplayName("주문 생성 성공 시 201을 반환한다")
    void createOrder_success() throws Exception {
        Orders order = Orders.create(
                "merchant-1",
                "buyer-1",
                "아이템",
                1000L
        );

        BDDMockito.given(
                consumerOrderFacade.createOrderWithPaymentCreated(
                        "merchant-1",
                        "buyer-1",
                        "아이템",
                        1000L
                )
        ).willReturn(order);

        String requestBody = """
                {
                  "merchantId": "merchant-1",
                  "buyerId": "buyer-1",
                  "itemName": "아이템",
                  "amount": 1000
                }
                """;

        mockMvc.perform(post("/api/consumer/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId", notNullValue()))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.amount").value(1000));
    }

    @Test
    @DisplayName("amount가 0 이하이면 400을 반환한다")
    void createOrder_invalidAmount() throws Exception {
        String requestBody = """
                {
                  "merchantId": "merchant-1",
                  "buyerId": "buyer-1",
                  "itemName": "아이템",
                  "amount": 0
                }
                """;

        mockMvc.perform(post("/api/consumer/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("amount가 null이면 400을 반환한다")
    void createOrder_nullAmount() throws Exception {
        String requestBody = """
                {
                  "merchantId": "merchant-1",
                  "buyerId": "buyer-1",
                  "itemName": "아이템",
                  "amount": null
                }
                """;

        mockMvc.perform(post("/api/consumer/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("merchantId가 비어 있으면 400을 반환한다")
    void createOrder_blankMerchantId() throws Exception {
        String requestBody = """
                {
                  "merchantId": "",
                  "buyerId": "buyer-1",
                  "itemName": "아이템",
                  "amount": 1000
                }
                """;

        mockMvc.perform(post("/api/consumer/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("buyerId가 비어 있으면 400을 반환한다")
    void createOrder_blankBuyerId() throws Exception {
        String requestBody = """
                {
                  "merchantId": "merchant-1",
                  "buyerId": "",
                  "itemName": "아이템",
                  "amount": 1000
                }
                """;

        mockMvc.perform(post("/api/consumer/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("itemName이 비어 있으면 400을 반환한다")
    void createOrder_blankItemName() throws Exception {
        String requestBody = """
                {
                  "merchantId": "merchant-1",
                  "buyerId": "buyer-1",
                  "itemName": "",
                  "amount": 1000
                }
                """;

        mockMvc.perform(post("/api/consumer/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("주문 생성 응답은 최소 필드만 반환한다")
    void createOrder_returnsMinimalResponse() throws Exception {
        Orders order = Orders.create(
                "merchant-1",
                "buyer-1",
                "아이템",
                1000L
        );

        BDDMockito.given(
                consumerOrderFacade.createOrderWithPaymentCreated(
                        "merchant-1",
                        "buyer-1",
                        "아이템",
                        1000L
                )
        ).willReturn(order);

        String requestBody = """
                {
                  "merchantId": "merchant-1",
                  "buyerId": "buyer-1",
                  "itemName": "아이템",
                  "amount": 1000
                }
                """;

        mockMvc.perform(post("/api/consumer/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId", notNullValue()))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.amount").value(1000))
                .andExpect(jsonPath("$.merchantId").doesNotExist())
                .andExpect(jsonPath("$.buyerId").doesNotExist())
                .andExpect(jsonPath("$.itemName").doesNotExist())
                .andExpect(jsonPath("$.currency").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.requestId").doesNotExist());
    }

    @Test
    @DisplayName("amount가 음수이면 400을 반환한다")
    void createOrder_negativeAmount() throws Exception {
        String requestBody = """
            {
              "merchantId": "merchant-1",
              "buyerId": "buyer-1",
              "itemName": "아이템",
              "amount": -1
            }
            """;

        mockMvc.perform(post("/api/consumer/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}