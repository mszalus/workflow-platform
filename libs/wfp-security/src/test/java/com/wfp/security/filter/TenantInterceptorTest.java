package com.wfp.security.filter;

import com.wfp.common.exception.ForbiddenException;
import com.wfp.security.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantInterceptorTest {

    private final TenantInterceptor interceptor = new TenantInterceptor();

    @AfterEach
    void clearContexts() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void takesTenantFromJwtAndIgnoresForgedHeader() {
        authenticateWith(jwt("tenant-a"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "tenant-b");

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo("tenant-a");
    }

    @Test
    void rejectsJwtWithoutTenantClaim() {
        authenticateWith(jwt(null));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "tenant-b");

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(ForbiddenException.class);
        assertThat(TenantContext.getCurrentTenantId()).isNull();
    }

    private static Jwt jwt(String tenantId) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("preferred_username", "user");
        if (tenantId != null) {
            builder.claim(TenantInterceptor.TENANT_CLAIM, tenantId);
        }
        return builder.build();
    }

    private static void authenticateWith(Jwt jwt) {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
