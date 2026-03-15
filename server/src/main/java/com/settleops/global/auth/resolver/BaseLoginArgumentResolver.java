package com.settleops.global.auth.resolver;

import com.settleops.global.error.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.context.request.NativeWebRequest;

public abstract class BaseLoginArgumentResolver {

    protected HttpSession getRequiredSession(NativeWebRequest webRequest) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        HttpSession session = request == null ? null : request.getSession(false);

        if (session == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        return session;
    }
}