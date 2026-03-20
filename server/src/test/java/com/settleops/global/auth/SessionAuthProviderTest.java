package com.settleops.global.auth;

import com.settleops.global.error.ForbiddenException;
import com.settleops.global.error.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionAuthProviderTest {

    private final SessionAuthProvider sessionAuthProvider = new SessionAuthProvider();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("anonymous authentication이면 401을 반환한다")
    void shouldThrowUnauthorized_whenAuthenticationIsAnonymous() {
        AnonymousAuthenticationToken authentication =
                new AnonymousAuthenticationToken(
                        "anonymous-key",
                        "anonymousUser",
                        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThatThrownBy(() -> sessionAuthProvider.getCurrentAdminId())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("인증 정보가 없습니다.");
    }

    @Test
    @DisplayName("인증은 있지만 admin 권한이 아니면 403을 반환한다")
    void shouldThrowForbidden_whenRoleDoesNotMatch() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "BUYER_1001",
                        null,
                        AuthorityUtils.createAuthorityList("ROLE_CONSUMER")
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThatThrownBy(() -> sessionAuthProvider.getCurrentAdminId())
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("admin 권한이 없습니다.");
    }

    @Test
    @DisplayName("principal이 blank면 401을 반환한다")
    void shouldThrowUnauthorized_whenPrincipalIsBlank() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        " ",
                        null,
                        AuthorityUtils.createAuthorityList("ROLE_ADMIN")
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThatThrownBy(() -> sessionAuthProvider.getCurrentAdminId())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("인증 정보가 없습니다.");
    }
}