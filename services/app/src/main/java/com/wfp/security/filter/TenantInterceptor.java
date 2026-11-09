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

import java.util.List;

@Slf4j
@Component
public class TenantInterceptor implements HandlerInterceptor {

    public static final String ORGANIZATION_CLAIM = "organization";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwtAuth)) {
            return true;
        }

        Jwt jwt = jwtAuth.getToken();
        String userId = jwt.getClaimAsString("preferred_username");
        List<String> organizations = jwt.getClaimAsStringList(ORGANIZATION_CLAIM);

        if (organizations == null || organizations.isEmpty()) {
            log.warn("Rejected token for '{}' without a '{}' claim", userId, ORGANIZATION_CLAIM);
            throw new ForbiddenException("Token has no tenant");
        }
        if (organizations.size() > 1) {
            log.warn("Rejected token for '{}' with several organizations", userId);
            throw new ForbiddenException("Token belongs to several organizations");
        }

        String tenantId = organizations.getFirst();
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
