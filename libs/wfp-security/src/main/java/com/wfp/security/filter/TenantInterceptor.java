package com.wfp.security.filter;

import com.wfp.security.context.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class TenantInterceptor implements HandlerInterceptor {

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String TENANT_CLAIM = "tenant_id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = request.getHeader(TENANT_HEADER);

        if ((tenantId == null || tenantId.isBlank())
                && SecurityContextHolder.getContext().getAuthentication()
                instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            tenantId = jwt.getClaimAsString(TENANT_CLAIM);
        }

        if (tenantId != null && !tenantId.isBlank()) {
            TenantContext.setCurrentTenantId(tenantId);
            MDC.put("tenantId", tenantId);
            log.debug("Set tenant context to: {}", tenantId);
        }

        // Populate userId MDC field for log correlation
        if (SecurityContextHolder.getContext().getAuthentication()
                instanceof JwtAuthenticationToken jwtAuth) {
            String userId = jwtAuth.getToken().getClaimAsString("preferred_username");
            if (userId != null) {
                MDC.put("userId", userId);
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        TenantContext.clear();
        MDC.remove("tenantId");
        MDC.remove("userId");
    }
}
