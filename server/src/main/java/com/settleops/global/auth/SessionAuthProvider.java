package com.settleops.global.auth;

import com.settleops.global.auth.controller.MeController;
import com.settleops.global.error.ForbiddenException;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class SessionAuthProvider {

    public String getRequiredMerchantId(HttpSession session) {
        String role = (String) session.getAttribute(MeController.SessionKeys.ROLE);
        String merchantId = (String) session.getAttribute(MeController.SessionKeys.MERCHANT_ID);

        if (!"MERCHANT".equals(role) || merchantId == null || merchantId.isBlank()) {
            throw new ForbiddenException("merchant 권한이 없습니다.");
        }
        return merchantId;
    }

    public String getRequiredBuyerId(HttpSession session) {
        String role = (String) session.getAttribute(MeController.SessionKeys.ROLE);
        String buyerId = (String) session.getAttribute(MeController.SessionKeys.BUYER_ID);

        if (!"CONSUMER".equals(role) || buyerId == null || buyerId.isBlank()) {
            throw new ForbiddenException("consumer 권한이 없습니다.");
        }
        return buyerId;
    }

    public void requireAdmin(HttpSession session) {
        String role = (String) session.getAttribute(MeController.SessionKeys.ROLE);

        if (!"ADMIN".equals(role)) {
            throw new ForbiddenException("admin 권한이 없습니다.");
        }
    }
}