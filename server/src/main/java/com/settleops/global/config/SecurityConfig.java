package com.settleops.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Profile("!test")
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

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

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/info", "/actuator/metrics/**", "/actuator/prometheus").permitAll()
                .requestMatchers("/error", "/api/health").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/**", "/api/me").permitAll()
                .anyRequest().authenticated()
        );

        return http.build();
    }
}