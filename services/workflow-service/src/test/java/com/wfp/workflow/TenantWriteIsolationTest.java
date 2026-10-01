package com.wfp.workflow;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantWriteIsolationTest {

    private static final String BPMN = """
        <?xml version="1.0" encoding="UTF-8"?>
        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                     targetNamespace="http://wfp.com/test">
          <process id="writeIsolation" name="Write Isolation" isExecutable="true">
            <startEvent id="start"/>
            <userTask id="review" name="Review"/>
            <endEvent id="end"/>
            <sequenceFlow sourceRef="start" targetRef="review"/>
            <sequenceFlow sourceRef="review" targetRef="end"/>
          </process>
        </definitions>
        """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService flowableTaskService;

    private String deploymentId;
    private String processInstanceId;
    private String taskId;

    @BeforeEach
    void startProcessInTenantA() {
        deploymentId = repositoryService.createDeployment()
                .addString("write-isolation.bpmn20.xml", BPMN)
                .tenantId("tenant-a")
                .deploy()
                .getId();
        processInstanceId = runtimeService.startProcessInstanceByKeyAndTenantId("writeIsolation", "tenant-a").getId();
        taskId = flowableTaskService.createTaskQuery().processInstanceId(processInstanceId).singleResult().getId();
    }

    @AfterEach
    void deleteDeployment() {
        if (deploymentExists()) {
            repositoryService.deleteDeployment(deploymentId, true);
        }
    }

    @Test
    void otherTenantCannotClaimTask() throws Exception {
        perform(post("/api/tasks/{id}/claim", taskId), "tenant-b").andExpect(status().isNotFound());

        assertThat(task().getAssignee()).isNull();
    }

    @Test
    void otherTenantCannotUnclaimTask() throws Exception {
        flowableTaskService.claim(taskId, "user-a");

        perform(post("/api/tasks/{id}/unclaim", taskId), "tenant-b").andExpect(status().isNotFound());

        assertThat(task().getAssignee()).isEqualTo("user-a");
    }

    @Test
    void otherTenantCannotCompleteTask() throws Exception {
        perform(post("/api/tasks/{id}/complete", taskId), "tenant-b").andExpect(status().isNotFound());

        assertThat(task()).isNotNull();
    }

    @Test
    void otherTenantCannotDelegateTask() throws Exception {
        perform(post("/api/tasks/{id}/delegate", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"delegateToUserId\":\"user-b\"}"), "tenant-b")
                .andExpect(status().isNotFound());

        assertThat(task().getAssignee()).isNull();
    }

    @Test
    void otherTenantCannotCancelProcess() throws Exception {
        perform(delete("/api/processes/{id}", processInstanceId), "tenant-b").andExpect(status().isNotFound());

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).count()).isEqualTo(1);
    }

    @Test
    void otherTenantCannotDeleteDeployment() throws Exception {
        perform(delete("/api/deployments/{id}", deploymentId), "tenant-b").andExpect(status().isNotFound());

        assertThat(deploymentExists()).isTrue();
    }

    @Test
    void ownTenantCancelsProcess() throws Exception {
        perform(delete("/api/processes/{id}", processInstanceId), "tenant-a").andExpect(status().isNoContent());

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).count()).isZero();
    }

    @Test
    void ownTenantDeletesDeployment() throws Exception {
        perform(delete("/api/deployments/{id}", deploymentId), "tenant-a").andExpect(status().isNoContent());

        assertThat(deploymentExists()).isFalse();
    }

    @Test
    void ownTenantCompletesTask() throws Exception {
        perform(post("/api/tasks/{id}/complete", taskId), "tenant-a").andExpect(status().isNoContent());

        assertThat(task()).isNull();
    }

    private org.springframework.test.web.servlet.ResultActions perform(MockHttpServletRequestBuilder request, String tenantId)
            throws Exception {
        return mockMvc.perform(request.with(jwt().jwt(j -> j.claim("preferred_username", "user-" + tenantId)
                .claim("tenant_id", tenantId))));
    }

    private boolean deploymentExists() {
        return repositoryService.createDeploymentQuery().deploymentId(deploymentId).count() > 0;
    }

    private Task task() {
        return flowableTaskService.createTaskQuery().taskId(taskId).singleResult();
    }
}
