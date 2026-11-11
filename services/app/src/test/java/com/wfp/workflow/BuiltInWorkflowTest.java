package com.wfp.workflow;

import com.jayway.jsonpath.JsonPath;
import com.wfp.workflow.engine.Samples;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BuiltInWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RepositoryService repositoryService;

    private final String tenant = "tenant-" + UUID.randomUUID();

    @Test
    void aFreshTenantHasTheDefaultWorkflow() throws Exception {
        mockMvc.perform(get("/api/workflow/deployments").with(admin(tenant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.key=='default')].name").value("Default"))
                .andExpect(jsonPath("$[?(@.key=='default')].builtIn").value(true));
    }

    @Test
    void otherSharedDeploymentsStayHidden() throws Exception {
        Deployment stale = repositoryService.createDeployment()
                .name("SpringBootAutoDeployment")
                .addString("simple.bpmn20.xml", Samples.xml("valid/simple"))
                .deploy();
        try {
            mockMvc.perform(get("/api/workflow/deployments").with(admin(tenant)))
                    .andExpect(jsonPath("$[?(@.key=='simple')]").isEmpty());
            perform(post("/api/projects"), admin(tenant), """
                    {"key":"PROJ","name":"Platform","itemTypes":[{"name":"Task","workflowKey":"simple"}]}
                    """).andExpect(status().isNotFound());
        } finally {
            repositoryService.deleteDeployment(stale.getId(), true);
        }
    }

    @Test
    void itemsOnTheDefaultWorkflowMoveThroughAllFourColumns() throws Exception {
        createProjectOnDefault();
        perform(post("/api/items"), admin(tenant), """
                {"project":"PROJ","type":"Task","title":"Fix login"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusName").value("New"))
                .andExpect(jsonPath("$.statusCategory").value("OPEN"))
                .andExpect(jsonPath("$.transitions[0].name").value("Accept"));

        transition("accept").andExpect(jsonPath("$.statusName").value("To do"))
                .andExpect(jsonPath("$.statusCategory").value("TODO"));
        transition("startWork").andExpect(jsonPath("$.statusName").value("In progress"))
                .andExpect(jsonPath("$.statusCategory").value("IN_PROGRESS"));
        transition("finish").andExpect(jsonPath("$.statusName").value("Done"))
                .andExpect(jsonPath("$.statusCategory").value("DONE"));
        transition("reopen").andExpect(jsonPath("$.statusName").value("To do"));
    }

    @Test
    void anotherTenantCannotTouchItemsOnTheSharedWorkflow() throws Exception {
        createProjectOnDefault();
        perform(post("/api/items"), admin(tenant), """
                {"project":"PROJ","type":"Task","title":"Fix login"}
                """).andExpect(status().isCreated());

        perform(post("/api/items/PROJ-1/transitions"), admin("other-" + UUID.randomUUID()), """
                {"transitionId":"accept"}
                """).andExpect(status().isNotFound());
    }

    @Test
    void theDefaultWorkflowCanBeReadButNotDeployedOverOrDeleted() throws Exception {
        String list = mockMvc.perform(get("/api/workflow/deployments").with(admin(tenant)))
                .andReturn().getResponse().getContentAsString();
        String versionId = JsonPath.<List<String>>read(list, "$[?(@.key=='default')].id").getFirst();
        String deploymentId = JsonPath.<List<String>>read(list, "$[?(@.key=='default')].deploymentId").getFirst();

        mockMvc.perform(get("/api/workflow/deployments/{id}/bpmn", versionId).with(admin(tenant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bpmnXml").value(containsString("wfp:statusCategory=\"TODO\"")));

        String copy = Samples.xml("valid/simple").replace("id=\"simple\"", "id=\"default\"");
        perform(post("/api/workflow/deployments"), admin(tenant),
                "{\"name\":\"mine\",\"bpmnXml\":" + com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                        .writeValueAsString(copy) + "}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("built-in workflow")));

        mockMvc.perform(delete("/api/workflow/deployments/{id}", deploymentId).with(admin(tenant)))
                .andExpect(status().isNotFound());
    }

    private void createProjectOnDefault() throws Exception {
        perform(post("/api/projects"), admin(tenant), """
                {"key":"PROJ","name":"Platform","itemTypes":[{"name":"Task","workflowKey":"default"}]}
                """).andExpect(status().isCreated());
    }

    private ResultActions transition(String transitionId) throws Exception {
        ResultActions result = perform(post("/api/items/PROJ-1/transitions"), admin(tenant),
                "{\"transitionId\":\"" + transitionId + "\"}");
        MockHttpServletResponse response = result.andReturn().getResponse();
        assertThat(response.getStatus()).as(transitionId + ": " + response.getContentAsString()).isEqualTo(200);
        return result;
    }

    private static RequestPostProcessor admin(String tenantId) {
        return jwt().jwt(token -> token.claim("preferred_username", "admin").claim("organization", List.of(tenantId)))
                .authorities(TestRoles.ADMIN);
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, RequestPostProcessor who, String body)
            throws Exception {
        return mockMvc.perform(request.with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
