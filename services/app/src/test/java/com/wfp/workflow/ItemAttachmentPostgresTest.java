package com.wfp.workflow;

import com.jayway.jsonpath.JsonPath;
import com.wfp.security.context.TenantContext;
import com.wfp.workflow.engine.Samples;
import com.wfp.workflow.engine.flowable.DeploymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("pgtest")
class ItemAttachmentPostgresTest {

    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static {
        postgres.start();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeploymentService deploymentService;

    private final String tenant = "tenant-" + UUID.randomUUID();

    @Test
    void aFileAtTheSizeLimitRoundTripsThroughBytea() throws Exception {
        TenantContext.runAs(tenant, () -> deploymentService.deploy("simple", null, Samples.xml("valid/simple")));
        postJson("/api/projects", """
                {"key":"PROJ","name":"Platform","itemTypes":[{"name":"Task","workflowKey":"simple"}]}
                """);
        postJson("/api/items", """
                {"project":"PROJ","type":"Task","title":"Fix login"}
                """);
        byte[] bytes = new byte[10 * 1024 * 1024];
        new Random(42).nextBytes(bytes);

        String body = mockMvc.perform(multipart("/api/items/PROJ-1/attachments")
                        .file(new MockMultipartFile("file", "dump.bin", "application/octet-stream", bytes))
                        .with(admin()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/api/items/PROJ-1/attachments").with(admin()))
                .andExpect(jsonPath("$[0].sizeBytes").value(bytes.length));
        byte[] downloaded = mockMvc.perform(get("/api/items/PROJ-1/attachments/{id}/content", id).with(admin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(downloaded).isEqualTo(bytes);
    }

    private void postJson(String url, String body) throws Exception {
        mockMvc.perform(post(url).with(admin()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private RequestPostProcessor admin() {
        return jwt().jwt(token -> token.claim("preferred_username", "admin").claim("tenant_id", tenant))
                .authorities(TestRoles.ADMIN);
    }
}
