package com.wfp.gateway.config;

import com.wfp.gateway.filter.TenantHeaderFilter;
import com.wfp.security.config.JwtTenantConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Security configuration for the API Gateway.
 * <p>
 * This overrides the default {@code SecurityConfig} from wfp-security because the gateway
 * uses Spring Cloud Gateway MVC (servlet-based) and does not need the {@code TenantInterceptor}
 * or MVC-specific interceptors. Instead, tenant propagation is handled by {@link TenantHeaderFilter}
 * which injects the {@code X-Tenant-Id} header into downstream requests.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class GatewaySecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    @Primary
    public SecurityFilterChain gatewaySecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(new JwtTenantConverter()))
            );

        return http.build();
    }
}
