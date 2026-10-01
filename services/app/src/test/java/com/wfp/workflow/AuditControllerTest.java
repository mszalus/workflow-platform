package com.wfp.workflow;

import com.wfp.workflow.entity.AuditEntry;
import com.wfp.workflow.repository.AuditEntryRepository;
import com.wfp.security.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditEntryRepository auditEntryRepository;

    @BeforeEach
    void setUp() {
        TenantContext.runAs("tenant-test", () -> auditEntryRepository.save(AuditEntry.builder()
                .eventType("process.started")
                .entityType("PROCESS")
                .entityId("proc-123")
                .userId("user-a")
                .tenantId("tenant-test")
                .timestamp(Instant.now())
                .details("{\"key\":\"value\"}")
                .sourceService("app")
                .build()));
    }

    @AfterEach
    void tearDown() {
        TenantContext.runAs("tenant-test", auditEntryRepository::deleteAll);
    }

    @Test
    void shouldQueryAuditEntries() throws Exception {
        mockMvc.perform(get("/api/audit")
                        .with(jwt().jwt(j -> j.claim("preferred_username", "admin")
                                .claim("tenant_id", "tenant-test")))
                        .header("X-Tenant-Id", "tenant-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].eventType").value("process.started"))
                .andExpect(jsonPath("$.content[0].entityType").value("PROCESS"));
    }

    @Test
    void shouldFilterByEntityType() throws Exception {
        mockMvc.perform(get("/api/audit")
                        .with(jwt().jwt(j -> j.claim("preferred_username", "admin")
                                .claim("tenant_id", "tenant-test")))
                        .header("X-Tenant-Id", "tenant-test")
                        .param("entityType", "TASK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void shouldEnforceTenantIsolation() throws Exception {
        mockMvc.perform(get("/api/audit")
                        .with(jwt().jwt(j -> j.claim("preferred_username", "admin")
                                .claim("tenant_id", "tenant-other")))
                        .header("X-Tenant-Id", "tenant-other"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void shouldIgnoreForgedTenantHeader() throws Exception {
        mockMvc.perform(get("/api/audit")
                        .with(jwt().jwt(j -> j.claim("preferred_username", "admin")
                                .claim("tenant_id", "tenant-other")))
                        .header("X-Tenant-Id", "tenant-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void shouldRejectTokenWithoutTenantClaim() throws Exception {
        mockMvc.perform(get("/api/audit")
                        .with(jwt().jwt(j -> j.claim("preferred_username", "admin")))
                        .header("X-Tenant-Id", "tenant-test"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Token has no tenant"));
    }
}
