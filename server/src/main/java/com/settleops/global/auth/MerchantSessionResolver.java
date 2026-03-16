package com.settleops.global.auth;

import com.settleops.global.error.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class MerchantSessionResolver {

    static final String MERCHANT_ID_SESSION_KEY = "merchantId";

    public String resolveMerchantId(HttpServletRequest request) {
        if (request == null) {
            throw new UnauthorizedException("login required");
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new UnauthorizedException("login required");
        }

        Object merchantId = session.getAttribute(MERCHANT_ID_SESSION_KEY);
        if (!(merchantId instanceof String s) || s.isBlank()) {
            throw new UnauthorizedException("login required");
        }

        return s;
    }
}