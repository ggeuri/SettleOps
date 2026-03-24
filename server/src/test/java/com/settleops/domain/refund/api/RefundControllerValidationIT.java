package com.settleops.domain.refund.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test-db")
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RefundControllerValidationIT {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("U6 내 환불 목록 조회는 page가 음수면 400을 반환한다")
    void myRefunds_invalidPage_returns400() throws Exception {
        mockMvc.perform(
                        get("/api/me/refunds")
                                .with(user("merchant-user").roles("MERCHANT"))
                                .sessionAttr("merchantId", "merchant-1")
                                .param("page", "-1")
                                .param("size", "20")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("U6 내 환불 목록 조회는 size가 0이면 400을 반환한다")
    void myRefunds_invalidSizeZero_returns400() throws Exception {
        mockMvc.perform(
                        get("/api/me/refunds")
                                .with(user("merchant-user").roles("MERCHANT"))
                                .sessionAttr("merchantId", "merchant-1")
                                .param("page", "0")
                                .param("size", "0")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("U6 내 환불 목록 조회는 size가 100 초과면 400을 반환한다")
    void myRefunds_invalidSizeTooLarge_returns400() throws Exception {
        mockMvc.perform(
                        get("/api/me/refunds")
                                .with(user("merchant-user").roles("MERCHANT"))
                                .sessionAttr("merchantId", "merchant-1")
                                .param("page", "0")
                                .param("size", "101")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("U6 내 환불 목록 조회는 지원하지 않는 status면 400을 반환한다")
    void myRefunds_invalidStatus_returns400() throws Exception {
        mockMvc.perform(
                        get("/api/me/refunds")
                                .with(user("merchant-user").roles("MERCHANT"))
                                .sessionAttr("merchantId", "merchant-1")
                                .param("status", "DONE")
                                .param("page", "0")
                                .param("size", "20")
                )
                .andExpect(status().isBadRequest());
    }
}