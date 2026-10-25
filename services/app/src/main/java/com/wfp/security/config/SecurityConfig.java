package com.wfp.security.config;

import com.wfp.security.filter.TenantInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig implements WebMvcConfigurer {

    private static final String ADMIN = "ADMIN";

    private final TenantInterceptor tenantInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF disabled intentionally: all clients are SPAs that authenticate via Bearer tokens
            // in the Authorization header, not via cookies. Stateless JWT auth is inherently safe
            // against CSRF because cross-origin requests cannot read or forge the Authorization header.
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/projects").hasRole(ADMIN)
                .requestMatchers(HttpMethod.POST, "/api/fields/schemas").hasRole(ADMIN)
                .requestMatchers(HttpMethod.DELETE, "/api/fields/schemas/**").hasRole(ADMIN)
                .requestMatchers(HttpMethod.POST, "/api/workflow/deployments").hasRole(ADMIN)
                .requestMatchers(HttpMethod.DELETE, "/api/workflow/deployments/**").hasRole(ADMIN)
                .requestMatchers(HttpMethod.POST, "/api/workflow/workflows/validate").hasRole(ADMIN)
                .requestMatchers(HttpMethod.DELETE, "/api/workflow/processes/**").hasRole(ADMIN)
                .requestMatchers("/api/audit/**").hasRole(ADMIN)
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(new JwtTenantConverter()))
            );

        return http.build();
    }
}
