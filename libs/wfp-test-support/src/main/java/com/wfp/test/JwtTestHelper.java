package com.wfp.test;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class JwtTestHelper {

    private JwtTestHelper() {}

    public static RequestPostProcessor tenantJwt(String tenantId, String userId, String... roles) {
        Collection<GrantedAuthority> authorities = Arrays.stream(roles)
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());

        return jwt()
                .jwt(builder -> builder
                        .claim("tenant_id", tenantId)
                        .claim("preferred_username", userId)
                        .claim("sub", userId)
                        .claim("realm_access", Map.of("roles", List.of(roles)))
                )
                .authorities(authorities);
    }

    public static RequestPostProcessor tenantAJwt(String... roles) {
        return tenantJwt(TenantTestHelper.TEST_TENANT_A, "user-a", roles);
    }

    public static RequestPostProcessor tenantBJwt(String... roles) {
        return tenantJwt(TenantTestHelper.TEST_TENANT_B, "user-b", roles);
    }
}
