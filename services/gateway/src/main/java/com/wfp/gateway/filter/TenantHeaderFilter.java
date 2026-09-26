package com.wfp.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Servlet filter that extracts the {@code tenant_id} claim from the authenticated JWT
 * and adds an {@code X-Tenant-Id} header to the request. Because Spring Cloud Gateway MVC
 * forwards headers from the original request to downstream services, this effectively
 * propagates the tenant identity across the entire platform.
 * <p>
 * This filter runs after Spring Security authentication so that the JWT is already
 * validated and available in the {@code SecurityContextHolder}.
 */
@Slf4j
@Component
public class TenantHeaderFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String TENANT_CLAIM = "tenant_id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String tenantId = jwt.getClaimAsString(TENANT_CLAIM);

            if (tenantId != null && !tenantId.isBlank()) {
                log.debug("Propagating tenant ID to downstream: {}", tenantId);
                filterChain.doFilter(new TenantHeaderRequestWrapper(request, tenantId), response);
                return;
            } else {
                log.warn("JWT for principal '{}' does not contain a '{}' claim",
                        jwt.getClaimAsString("preferred_username"), TENANT_CLAIM);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Wraps the original request to inject the {@code X-Tenant-Id} header
     * without modifying the original request object.
     */
    private static class TenantHeaderRequestWrapper extends HttpServletRequestWrapper {

        private final String tenantId;

        TenantHeaderRequestWrapper(HttpServletRequest request, String tenantId) {
            super(request);
            this.tenantId = tenantId;
        }

        @Override
        public String getHeader(String name) {
            if (TENANT_HEADER.equalsIgnoreCase(name)) {
                return tenantId;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (TENANT_HEADER.equalsIgnoreCase(name)) {
                return Collections.enumeration(List.of(tenantId));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            var names = new LinkedHashSet<String>();
            var original = super.getHeaderNames();
            while (original.hasMoreElements()) {
                names.add(original.nextElement());
            }
            names.add(TENANT_HEADER);
            return Collections.enumeration(names);
        }
    }
}
