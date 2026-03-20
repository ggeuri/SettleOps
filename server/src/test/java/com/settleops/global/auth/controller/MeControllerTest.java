package com.settleops.global.auth.controller;

import com.settleops.global.audit.AuditLogger;
import com.settleops.global.auth.SessionAuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeController.class)
@AutoConfigureMockMvc(addFilters = false)
class MeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SessionAuthProvider sessionAuthProvider;

    @MockBean
    private AuditLogger auditLogger;

    @Test
    @DisplayName("인증이 없으면 /api/me 는 401을 반환한다")
    void me_shouldReturn401_whenAuthenticationDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(username = "ADMIN_1001", roles = "ADMIN")
    @DisplayName("admin 인증이면 /api/me 에 role과 adminId를 반환한다")
    void me_shouldReturnAdminInfo_whenAdminAuthenticationExists() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.buyerId").value(nullValue()))
                .andExpect(jsonPath("$.merchantId").value(nullValue()))
                .andExpect(jsonPath("$.adminId").value("ADMIN_1001"));
    }

    @Test
    @WithMockUser(username = "BUYER_1001", roles = "CONSUMER")
    @DisplayName("consumer 인증이면 /api/me 에 role과 buyerId를 반환한다")
    void me_shouldReturnConsumerInfo_whenConsumerAuthenticationExists() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CONSUMER"))
                .andExpect(jsonPath("$.buyerId").value("BUYER_1001"))
                .andExpect(jsonPath("$.merchantId").value(nullValue()))
                .andExpect(jsonPath("$.adminId").value(nullValue()));
    }
}