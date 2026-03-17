package com.settleops.global.auth.resolver;

import com.settleops.global.auth.SessionAuthProvider;
import com.settleops.global.auth.annotation.LoginMerchant;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class LoginMerchantArgumentResolver extends BaseLoginArgumentResolver
        implements HandlerMethodArgumentResolver {

    private final SessionAuthProvider sessionAuthProvider;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginMerchant.class)
                && String.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpSession session = getRequiredSession(webRequest);
        return sessionAuthProvider.getRequiredMerchantId(session);
    }
}