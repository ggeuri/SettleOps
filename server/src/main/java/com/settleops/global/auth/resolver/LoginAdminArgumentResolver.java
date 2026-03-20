package com.settleops.global.auth.resolver;

import com.settleops.global.auth.SessionAuthProvider;
import com.settleops.global.auth.annotation.LoginAdmin;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * SecurityContext에 주입된 현재 adminId를 반환한다.
 */
@Component
@RequiredArgsConstructor
public class LoginAdminArgumentResolver
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
        return sessionAuthProvider.getCurrentAdminId();
    }
}