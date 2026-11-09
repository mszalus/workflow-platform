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

import java.util.List;

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
        authenticateWith(jwt());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "tenant-b");

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(ForbiddenException.class);
        assertThat(TenantContext.getCurrentTenantId()).isNull();
    }

    @Test
    void rejectsJwtWithSeveralOrganizations() {
        authenticateWith(jwt("tenant-a", "tenant-b"));

        assertThatThrownBy(() -> interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(),
                new Object()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Token belongs to several organizations");
        assertThat(TenantContext.getCurrentTenantId()).isNull();
    }

    private static Jwt jwt(String... organizations) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("preferred_username", "user");
        if (organizations.length > 0) {
            builder.claim(TenantInterceptor.ORGANIZATION_CLAIM, List.of(organizations));
        }
        return builder.build();
    }

    private static void authenticateWith(Jwt jwt) {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
