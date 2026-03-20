package com.settleops.global.auth.controller;

import com.settleops.global.error.UnauthorizedException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MeController {

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken
                || auth.getName() == null
                || auth.getName().isBlank()) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        String role = extractRole(auth);
        String principal = auth.getName();

        return ResponseEntity.ok(
                switch (role) {
                    case "CONSUMER" -> new MeResponse(role, principal, null, null);
                    case "MERCHANT" -> new MeResponse(role, null, principal, null);
                    case "ADMIN" -> new MeResponse(role, null, null, principal);
                    default -> throw new UnauthorizedException("로그인이 필요합니다.");
                }
        );
    }

    private String extractRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .findFirst()
                .orElseThrow(() -> new UnauthorizedException("로그인이 필요합니다."));
    }

    public record MeResponse(String role, String buyerId, String merchantId, String adminId) {}

    /** 세션 키 하드코딩 방지 */
    public static final class SessionKeys {
        private SessionKeys() {}
        public static final String ROLE = "ROLE";
        public static final String BUYER_ID = "BUYER_ID";
        public static final String MERCHANT_ID = "MERCHANT_ID";
        public static final String ADMIN_ID = "ADMIN_ID";
    }
}