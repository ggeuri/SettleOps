package com.settleops.global.auth.resolver;

import com.settleops.global.auth.SessionAuthProvider;
import com.settleops.global.auth.annotation.LoginAdmin;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 현재 기획 범위에서는 Admin의 개별 세션 식별값(adminId)을 정의하지 않는다.
 * 따라서 ROLE=ADMIN 여부만 검증한 뒤 role 값을 주입한다.
 *
 * 향후 admin 세션 식별값 정책이 확정되면
 * 반환값을 adminId 또는 AdminPrincipal 객체로 확장한다.
 */
@Component
@RequiredArgsConstructor
public class LoginAdminArgumentResolver extends BaseLoginArgumentResolver
        implements HandlerMethodArgumentResolver {

    private final SessionAuthProvider sessionAuthProvider;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginAdmin.class)
                && String.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpSession session = getRequiredSession(webRequest);
        sessionAuthProvider.requireAdmin(session);
        return "ADMIN";
    }
}