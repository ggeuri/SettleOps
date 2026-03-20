package com.settleops.global.auth;

import com.settleops.global.auth.controller.MeController;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class SessionAuthenticationFilterTest {

    private final SessionAuthenticationFilter filter = new SessionAuthenticationFilter();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("admin 세션이 있으면 SecurityContext에 adminId와 ROLE_ADMIN 권한을 주입한다")
    void shouldPopulateSecurityContext_whenAdminSessionExists() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = new MockHttpSession();

        session.setAttribute(MeController.SessionKeys.ROLE, "ADMIN");
        session.setAttribute(MeController.SessionKeys.ADMIN_ID, "ADMIN_1001");
        request.setSession(session);

        AtomicReference<Authentication> observedAuthentication = new AtomicReference<>();

        FilterChain chain = (req, res) ->
                observedAuthentication.set(SecurityContextHolder.getContext().getAuthentication());

        filter.doFilter(request, response, chain);

        Authentication authentication = observedAuthentication.get();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("ADMIN_1001");
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("세션이 없으면 SecurityContext를 비운 상태로 다음 체인으로 진행한다")
    void shouldNotPopulateSecurityContext_whenSessionDoesNotExist() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<Authentication> observedAuthentication = new AtomicReference<>();

        FilterChain chain = (req, res) ->
                observedAuthentication.set(SecurityContextHolder.getContext().getAuthentication());

        filter.doFilter(request, response, chain);

        assertThat(observedAuthentication.get()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}