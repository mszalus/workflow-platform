package com.wfp.security.filter;

import com.wfp.common.exception.ForbiddenException;
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

    public static final String TENANT_CLAIM = "tenant_id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwtAuth)) {
            return true;
        }

        Jwt jwt = jwtAuth.getToken();
        String userId = jwt.getClaimAsString("preferred_username");
        String tenantId = jwt.getClaimAsString(TENANT_CLAIM);

        if (tenantId == null || tenantId.isBlank()) {
            log.warn("Rejected token for '{}' without a '{}' claim", userId, TENANT_CLAIM);
            throw new ForbiddenException("Token has no tenant");
        }

        TenantContext.setCurrentTenantId(tenantId);
        MDC.put("tenantId", tenantId);
        if (userId != null) {
            MDC.put("userId", userId);
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
