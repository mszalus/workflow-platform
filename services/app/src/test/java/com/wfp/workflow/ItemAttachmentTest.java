package com.wfp.workflow;

import com.jayway.jsonpath.JsonPath;
import com.wfp.security.context.TenantContext;
import com.wfp.workflow.engine.Samples;
import com.wfp.workflow.engine.flowable.DeploymentService;
import org.flowable.engine.RepositoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ItemAttachmentTest {

    private static final byte[] PDF_BYTES = {0x25, 0x50, 0x44, 0x46, 0x00, (byte) 0xFF};

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeploymentService deploymentService;

    @Autowired
    private RepositoryService repositoryService;

    private final String tenant = "tenant-" + UUID.randomUUID();

    @BeforeEach
    void createItem() throws Exception {
        TenantContext.runAs(tenant, () -> deploymentService.deploy("simple", null, Samples.xml("valid/simple")));
        perform(post("/api/projects"), "admin", """
                {"key":"PROJ","name":"Platform","itemTypes":[{"name":"Task","workflowKey":"simple"}]}
                """).andExpect(status().isCreated());
        perform(post("/api/items"), "alice", """
                {"project":"PROJ","type":"Task","title":"Fix login"}
                """).andExpect(status().isCreated());
    }

    @AfterEach
    void deleteDeployments() {
        repositoryService.createDeploymentQuery().deploymentTenantId(tenant).list()
                .forEach(deployment -> repositoryService.deleteDeployment(deployment.getId(), true));
    }

    @Test
    void uploadsListsAndDownloadsTheExactBytes() throws Exception {
        String id = upload("report.pdf", "application/pdf", PDF_BYTES, "alice");

        mockMvc.perform(get("/api/items/PROJ-1/attachments").with(user("bob")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fileName").value("report.pdf"))
                .andExpect(jsonPath("$[0].sizeBytes").value(PDF_BYTES.length))
                .andExpect(jsonPath("$[0].uploadedBy").value("alice"))
                .andExpect(jsonPath("$[0].content").doesNotExist());

        byte[] downloaded = mockMvc.perform(get("/api/items/PROJ-1/attachments/{id}/content", id).with(user("bob")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.startsWith("attachment;")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(downloaded).isEqualTo(PDF_BYTES);
    }

    @Test
    void keepsOnlyTheFileNameFromAPath() throws Exception {
        upload("..\\..\\secret/evil.txt", "text/plain", new byte[] {1}, "alice");

        mockMvc.perform(get("/api/items/PROJ-1/attachments").with(user("alice")))
                .andExpect(jsonPath("$[0].fileName").value("evil.txt"));
    }

    @Test
    void refusesAnEmptyOrOversizedFile() throws Exception {
        uploadRequest("empty.txt", "text/plain", new byte[0], user("alice")).andExpect(status().isBadRequest());

        uploadRequest("big.bin", "application/octet-stream", new byte[10 * 1024 * 1024 + 1], user("alice"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Files can be at most 10 MB"));
    }

    @Test
    void onlyTheUploaderOrAnAdminCanDelete() throws Exception {
        String id = upload("report.pdf", "application/pdf", PDF_BYTES, "alice");

        mockMvc.perform(delete("/api/items/PROJ-1/attachments/{id}", id).with(user("bob")))
                .andExpect(status().isForbidden());
        perform(delete("/api/items/PROJ-1/attachments/{id}", id), "admin", null).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/items/PROJ-1/attachments").with(user("alice")))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void theUploaderCanDeleteTheirOwnFile() throws Exception {
        String id = upload("report.pdf", "application/pdf", PDF_BYTES, "alice");

        mockMvc.perform(delete("/api/items/PROJ-1/attachments/{id}", id).with(user("alice")))
                .andExpect(status().isNoContent());
    }

    @Test
    void anAttachmentCannotBeReachedThroughAnotherItem() throws Exception {
        String id = upload("report.pdf", "application/pdf", PDF_BYTES, "alice");
        perform(post("/api/items"), "alice", """
                {"project":"PROJ","type":"Task","title":"Other"}
                """).andExpect(status().isCreated());

        mockMvc.perform(get("/api/items/PROJ-2/attachments/{id}/content", id).with(user("alice")))
                .andExpect(status().isNotFound());
    }

    @Test
    void anotherTenantCannotListDownloadUploadOrDelete() throws Exception {
        String id = upload("report.pdf", "application/pdf", PDF_BYTES, "alice");
        RequestPostProcessor mallory = jwt().jwt(token -> token.claim("preferred_username", "mallory")
                .claim("tenant_id", "other-" + UUID.randomUUID())).authorities(TestRoles.ADMIN);

        mockMvc.perform(get("/api/items/PROJ-1/attachments").with(mallory)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/items/PROJ-1/attachments/{id}/content", id).with(mallory))
                .andExpect(status().isNotFound());
        uploadRequest("x.txt", "text/plain", new byte[] {1}, mallory).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/items/PROJ-1/attachments/{id}", id).with(mallory))
                .andExpect(status().isNotFound());
    }

    private String upload(String name, String type, byte[] bytes, String uploader) throws Exception {
        String body = uploadRequest(name, type, bytes, user(uploader))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private ResultActions uploadRequest(String name, String type, byte[] bytes, RequestPostProcessor who)
            throws Exception {
        return mockMvc.perform(multipart("/api/items/PROJ-1/attachments")
                .file(new MockMultipartFile("file", name, type, bytes))
                .with(who));
    }

    private RequestPostProcessor user(String name) {
        return jwt().jwt(token -> token.claim("preferred_username", name).claim("tenant_id", tenant))
                .authorities(TestRoles.USER);
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String name, String body) throws Exception {
        request.with(jwt().jwt(token -> token.claim("preferred_username", name).claim("tenant_id", tenant))
                .authorities(TestRoles.ADMIN));
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request);
    }
}
