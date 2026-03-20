package com.settleops.global.auth;

import com.settleops.global.auth.controller.MeController;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            HttpSession session = request.getSession(false);

            if (session != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                String role = getSessionString(session, MeController.SessionKeys.ROLE);
                String principal = resolvePrincipal(session, role);

                if (role != null && principal != null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
                            );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String resolvePrincipal(HttpSession session, String role) {
        if (role == null) {
            return null;
        }

        return switch (role) {
            case "CONSUMER" -> getSessionString(session, MeController.SessionKeys.BUYER_ID);
            case "MERCHANT" -> getSessionString(session, MeController.SessionKeys.MERCHANT_ID);
            case "ADMIN" -> getSessionString(session, MeController.SessionKeys.ADMIN_ID);
            default -> null;
        };
    }

    private String getSessionString(HttpSession session, String key) {
        Object value = session.getAttribute(key);

        if (!(value instanceof String str) || str.isBlank()) {
            return null;
        }

        return str;
    }
}