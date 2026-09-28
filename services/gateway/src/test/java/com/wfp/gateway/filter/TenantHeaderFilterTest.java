package com.wfp.gateway.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class TenantHeaderFilterTest {

    @Test
    void stripsClientSuppliedTenantHeaderAndKeepsOthers() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/workflow/deployments");
        request.addHeader("x-tenant-id", "tenant-b");
        request.addHeader("Authorization", "Bearer token");
        MockFilterChain chain = new MockFilterChain();

        new TenantHeaderFilter().doFilter(request, new MockHttpServletResponse(), chain);

        HttpServletRequest forwarded = (HttpServletRequest) chain.getRequest();
        assertThat(forwarded.getHeader("X-Tenant-Id")).isNull();
        assertThat(Collections.list(forwarded.getHeaders("X-Tenant-Id"))).isEmpty();
        assertThat(Collections.list(forwarded.getHeaderNames()))
                .noneMatch("X-Tenant-Id"::equalsIgnoreCase)
                .contains("Authorization");
        assertThat(forwarded.getHeader("Authorization")).isEqualTo("Bearer token");
    }
}
