package com.wfp.workflow;

import com.wfp.security.context.TenantContext;
import com.wfp.workflow.engine.Samples;
import com.wfp.workflow.engine.flowable.DeploymentService;
import com.wfp.workflow.entity.NotificationType;
import com.wfp.workflow.service.AuditService;
import com.wfp.workflow.service.NotificationService;
import org.flowable.engine.RepositoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ItemRollbackTest {

    private static final String ITEM = """
        {"project":"PROJ","type":"Task","title":"Fix login","assignee":"bob"}
        """;
    private static final String COMMENT = "{\"content\":\"Hello\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeploymentService deploymentService;

    @Autowired
    private RepositoryService repositoryService;

    @MockitoSpyBean
    private NotificationService notificationService;

    @MockitoSpyBean
    private AuditService auditService;

    private final String tenant = "tenant-" + UUID.randomUUID();

    @BeforeEach
    void createProject() throws Exception {
        TenantContext.runAs(tenant, () -> deploymentService.deploy("simple", null, Samples.xml("valid/simple")));
        perform(post("/api/projects"), "admin", """
                {"key":"PROJ","name":"Platform","itemTypes":[{"name":"Task","workflowKey":"simple"}]}
                """).andExpect(status().isCreated());
    }

    @AfterEach
    void deleteDeployments() {
        repositoryService.createDeploymentQuery().deploymentTenantId(tenant).list()
                .forEach(deployment -> repositoryService.deleteDeployment(deployment.getId(), true));
    }

    @Test
    void aFailedNotificationRollsBackTheComment() throws Exception {
        perform(post("/api/items"), "alice", ITEM).andExpect(status().isCreated());
        doThrow(new IllegalStateException("notification store down")).when(notificationService)
                .createNotification(any(), any(), any(), any(), eq(NotificationType.ITEM_COMMENTED), any(), any());

        perform(post("/api/items/PROJ-1/comments"), "carol", COMMENT).andExpect(status().is5xxServerError());

        perform(get("/api/items/PROJ-1/comments"), "alice", null).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void aFailedAuditEntryRollsBackTheItem() throws Exception {
        doThrow(new IllegalStateException("audit store down")).when(auditService)
                .record(eq("item.created"), any(), any(), any(), any());

        perform(post("/api/items"), "alice", ITEM).andExpect(status().is5xxServerError());

        perform(get("/api/items"), "alice", null).andExpect(jsonPath("$.totalElements").value(0));
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String user, String body) throws Exception {
        request.with(jwt().jwt(token -> token.claim("preferred_username", user).claim("tenant_id", tenant))
                .authorities("admin".equals(user) ? TestRoles.ADMIN : TestRoles.USER));
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request);
    }
}
