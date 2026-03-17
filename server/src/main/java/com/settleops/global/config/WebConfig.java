package com.settleops.global.config;

import com.settleops.global.auth.resolver.LoginAdminArgumentResolver;
import com.settleops.global.auth.resolver.LoginConsumerArgumentResolver;
import com.settleops.global.auth.resolver.LoginMerchantArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final LoginMerchantArgumentResolver loginMerchantArgumentResolver;
    private final LoginConsumerArgumentResolver loginConsumerArgumentResolver;
    private final LoginAdminArgumentResolver loginAdminArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginMerchantArgumentResolver);
        resolvers.add(loginConsumerArgumentResolver);
        resolvers.add(loginAdminArgumentResolver);
    }
}