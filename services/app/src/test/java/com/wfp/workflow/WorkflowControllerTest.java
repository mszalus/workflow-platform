package com.wfp.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wfp.workflow.engine.Samples;
import org.flowable.engine.RepositoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkflowControllerTest {

    private static final String PLAIN_PROCESS = """
        <?xml version="1.0" encoding="UTF-8"?>
        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" targetNamespace="http://wfp.com/test">
          <process id="plainDraft" isExecutable="false">
            <startEvent id="start"/>
            <userTask id="review" name="Review"/>
            <endEvent id="end"/>
            <sequenceFlow id="f1" sourceRef="start" targetRef="review"/>
            <sequenceFlow id="f2" sourceRef="review" targetRef="end"/>
          </process>
        </definitions>
        """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RepositoryService repositoryService;

    private final String tenant = "tenant-" + UUID.randomUUID();

    @AfterEach
    void deleteDeployments() {
        repositoryService.createDeploymentQuery().deploymentTenantId(tenant).list()
                .forEach(deployment -> repositoryService.deleteDeployment(deployment.getId(), true));
    }

    @Test
    void aValidTrackerWorkflowHasNoViolations() throws Exception {
        validate(Samples.xml("valid/bug-flow")).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void reportsEachViolationWithItsElementAndRule() throws Exception {
        validate(Samples.xml("invalid/rule4-unnamed-transition"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].elementId").value("skip"))
                .andExpect(jsonPath("$[0].rule").value(4))
                .andExpect(jsonPath("$[0].message").isNotEmpty());
    }

    @Test
    void unreadableXmlIsOneViolationWithoutAnElement() throws Exception {
        validate("this is not BPMN")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].elementId").doesNotExist())
                .andExpect(jsonPath("$[0].rule").value(0));
    }

    @Test
    void deployRefusesAnInvalidTrackerWorkflowWithItsViolations() throws Exception {
        deploy(Samples.xml("invalid/rule1-multi-instance-status"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.violations[*].elementId", containsInAnyOrder("review")))
                .andExpect(jsonPath("$.details.violations[0].rule").value(1));

        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(tenant).count()).isZero();
    }

    @Test
    void deployStillAcceptsAPlainProcessAndMakesItExecutable() throws Exception {
        deploy(PLAIN_PROCESS).andExpect(status().isCreated());

        assertThat(repositoryService.createProcessDefinitionQuery()
                .processDefinitionTenantId(tenant)
                .processDefinitionKey("plainDraft")
                .count()).isEqualTo(1);
    }

    @Test
    void deployRefusesUnreadableXml() throws Exception {
        deploy("this is not BPMN")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The workflow is not readable BPMN XML"));
    }

    private ResultActions validate(String bpmnXml) throws Exception {
        return send("/api/workflow/workflows/validate", Map.of("bpmnXml", bpmnXml));
    }

    private ResultActions deploy(String bpmnXml) throws Exception {
        return send("/api/workflow/deployments", Map.of("name", "draft", "bpmnXml", bpmnXml));
    }

    private ResultActions send(String path, Map<String, String> body) throws Exception {
        return mockMvc.perform(post(path)
                .with(jwt().jwt(token -> token.claim("preferred_username", "admin").claim("tenant_id", tenant)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }
}
