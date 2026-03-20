package com.settleops.global.auth;

import com.settleops.global.audit.AuditLogger;
import com.settleops.global.auth.controller.MeController;
import com.settleops.global.error.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.hamcrest.Matchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.settleops.global.auth.controller.MeController.SessionKeys.ADMIN_ID;
import static com.settleops.global.auth.controller.MeController.SessionKeys.BUYER_ID;
import static com.settleops.global.auth.controller.MeController.SessionKeys.MERCHANT_ID;
import static com.settleops.global.auth.controller.MeController.SessionKeys.ROLE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        MeController.class,
        SessionSecurityContextIntegrationTest.TestProtectedController.class
})
@AutoConfigureMockMvc(addFilters = true)
@Import({
        SessionAuthenticationFilter.class,
        SessionAuthProvider.class,
        GlobalExceptionHandler.class,
        SessionSecurityContextIntegrationTest.TestSecurityConfig.class,
        SessionSecurityContextIntegrationTest.TestProtectedController.class
})
class SessionSecurityContextIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogger auditLogger;

    @Test
    @DisplayName("동일 consumer 세션이면 /api/me 와 consumer 보호 API가 동일 principal/role 기준으로 동작")
    void consumerSession_shouldBeResolvedConsistently_acrossMeAndProtectedApi() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(ROLE, "CONSUMER");
        session.setAttribute(BUYER_ID, "BUYER_1001");

        mockMvc.perform(get("/api/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CONSUMER"))
                .andExpect(jsonPath("$.buyerId").value("BUYER_1001"))
                .andExpect(jsonPath("$.merchantId").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.adminId").value(Matchers.nullValue()));

        mockMvc.perform(get("/api/test/consumer-only").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string("BUYER_1001"));

        mockMvc.perform(get("/api/test/merchant-only").session(session))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/test/admin-only").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("동일 merchant 세션이면 /api/me 와 merchant 보호 API가 동일 principal/role 기준으로 동작")
    void merchantSession_shouldBeResolvedConsistently_acrossMeAndProtectedApi() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(ROLE, "MERCHANT");
        session.setAttribute(MERCHANT_ID, "MERCHANT_1001");

        mockMvc.perform(get("/api/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MERCHANT"))
                .andExpect(jsonPath("$.buyerId").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.merchantId").value("MERCHANT_1001"))
                .andExpect(jsonPath("$.adminId").value(Matchers.nullValue()));

        mockMvc.perform(get("/api/test/merchant-only").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string("MERCHANT_1001"));
    }

    @Test
    @DisplayName("동일 admin 세션이면 /api/me 와 admin 보호 API가 동일 principal/role 기준으로 동작")
    void adminSession_shouldBeResolvedConsistently_acrossMeAndProtectedApi() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(ROLE, "ADMIN");
        session.setAttribute(ADMIN_ID, "ADMIN_1001");

        mockMvc.perform(get("/api/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.buyerId").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.merchantId").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.adminId").value("ADMIN_1001"));

        mockMvc.perform(get("/api/test/admin-only").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string("ADMIN_1001"));
    }

    @Test
    @DisplayName("세션이 없으면 /api/me 와 보호 API 모두 401로 동작")
    void noSession_shouldReturn401_acrossMeAndProtectedApi() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/test/consumer-only"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @RestController
    @RequestMapping("/api/test")
    static class TestProtectedController {

        private final SessionAuthProvider sessionAuthProvider;

        TestProtectedController(SessionAuthProvider sessionAuthProvider) {
            this.sessionAuthProvider = sessionAuthProvider;
        }

        @GetMapping("/consumer-only")
        public String consumerOnly() {
            return sessionAuthProvider.getCurrentConsumerId();
        }

        @GetMapping("/merchant-only")
        public String merchantOnly() {
            return sessionAuthProvider.getCurrentMerchantId();
        }

        @GetMapping("/admin-only")
        public String adminOnly() {
            return sessionAuthProvider.getCurrentAdminId();
        }
    }

    @TestConfiguration
    static class TestSecurityConfig {

        @Autowired
        private SessionAuthenticationFilter sessionAuthenticationFilter;

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable());
            http.formLogin(form -> form.disable());
            http.httpBasic(basic -> basic.disable());

            http.addFilterBefore(sessionAuthenticationFilter, AnonymousAuthenticationFilter.class);

            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

            return http.build();
        }
    }
}