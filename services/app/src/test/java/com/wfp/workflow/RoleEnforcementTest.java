package com.wfp.workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoleEnforcementTest {

    private static final String TENANT = "tenant-roles";

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @CsvSource({
            "POST,   /api/projects",
            "POST,   /api/fields/schemas",
            "DELETE, /api/fields/schemas/1",
            "POST,   /api/workflow/deployments",
            "DELETE, /api/workflow/deployments/any",
            "POST,   /api/workflow/workflows/validate",
            "DELETE, /api/workflow/processes/any",
            "GET,    /api/audit",
    })
    void refusesTenantAdministrationToAPlainUser(String method, String path) throws Exception {
        mockMvc.perform(authenticated(method, path, TestRoles.USER))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @CsvSource({
            "POST,   /api/projects",
            "POST,   /api/fields/schemas",
            "DELETE, /api/fields/schemas/1",
            "POST,   /api/workflow/deployments",
            "DELETE, /api/workflow/deployments/any",
            "POST,   /api/workflow/workflows/validate",
            "DELETE, /api/workflow/processes/any",
            "GET,    /api/audit",
    })
    void letsAnAdminPastTheRoleGate(String method, String path) throws Exception {
        mockMvc.perform(authenticated(method, path, TestRoles.ADMIN))
                .andExpect(result -> {
                    if (result.getResponse().getStatus() == 403) {
                        throw new AssertionError(method + " " + path + " refused an admin");
                    }
                });
    }

    @ParameterizedTest
    @CsvSource({
            "GET, /api/items",
            "GET, /api/projects",
            "GET, /api/fields/schemas",
            "GET, /api/notifications",
            "GET, /api/workflow/deployments",
            "GET, /api/workflow/tasks",
    })
    void leavesTheScreensAPlainUserNeedsOpen(String method, String path) throws Exception {
        mockMvc.perform(authenticated(method, path, TestRoles.USER))
                .andExpect(status().isOk());
    }

    @Test
    void stillRejectsAnUnauthenticatedCall() throws Exception {
        mockMvc.perform(request(HttpMethod.GET, "/api/items")).andExpect(status().isUnauthorized());
    }

    private MockHttpServletRequestBuilder authenticated(String method, String path,
                                                        List<org.springframework.security.core.GrantedAuthority> roles) {
        return request(HttpMethod.valueOf(method), path.trim())
                .with(jwt().jwt(token -> token.claim("preferred_username", "someone")
                        .claim("tenant_id", TENANT)).authorities(roles))
                .contentType("application/json")
                .content("{}");
    }
}
