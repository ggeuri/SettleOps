package com.settleops.global.config;

import com.settleops.global.auth.SessionAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.List;

@Profile("!test")
@Configuration
public class SecurityConfig {

    private final SessionAuthenticationFilter sessionAuthenticationFilter;

    public SecurityConfig(SessionAuthenticationFilter sessionAuthenticationFilter) {
        this.sessionAuthenticationFilter = sessionAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, Environment env) throws Exception {

        http.csrf(csrf -> csrf.disable());
        http.formLogin(form -> form.disable());
        http.httpBasic(basic -> basic.disable());

        http.cors(cors -> cors.configurationSource(request -> {
            CorsConfiguration config = new CorsConfiguration();

            config.setAllowedOriginPatterns(List.of("http://localhost:*"));
            config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
            config.setAllowedHeaders(List.of(
                    "Content-Type","X-Request-Id","X-Idempotency-Key","Accept","Origin","X-Requested-With"
            ));
            config.setExposedHeaders(List.of("X-Request-Id"));
            config.setAllowCredentials(true);
            config.setMaxAge(3600L);

            return config;
        }));

        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler(new AccessDeniedHandlerImpl()) // 기본 403
        );

        // local/dev 여부 판단 (이중 안전장치)
        boolean devOrLocal = Arrays.asList(env.getActiveProfiles()).contains("dev")
                || Arrays.asList(env.getActiveProfiles()).contains("local");

        http.authorizeHttpRequests(auth -> {
            // /api/dev/**는 local/dev에서만 허용, 그 외 환경은 차단
            if (devOrLocal) {
                auth.requestMatchers("/api/dev/**").permitAll();
            } else {
                auth.requestMatchers("/api/dev/**").denyAll();
            }

            // 나머지 정책
            auth.requestMatchers("/actuator/health").permitAll();
            auth.requestMatchers("/actuator/info", "/actuator/metrics/**", "/actuator/prometheus").permitAll();
            auth.requestMatchers("/error", "/api/health").permitAll();
            auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
            auth.requestMatchers("/api/auth/**", "/api/me").permitAll();
            auth.anyRequest().authenticated();
        });

        http.addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}