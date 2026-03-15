package com.settleops.global.auth;

import com.settleops.global.auth.controller.MeController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class MerchantSessionResolver {

    public String resolveMerchantId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required");
        }

        Object merchantId = session.getAttribute(MeController.SessionKeys.MERCHANT_ID);
        if (!(merchantId instanceof String s) || s.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "merchant login required");
        }

        return s;
    }
}