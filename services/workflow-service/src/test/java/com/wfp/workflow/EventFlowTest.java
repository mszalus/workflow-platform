package com.wfp.workflow;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventFlowTest {

    private static final String BPMN = """
        <?xml version="1.0" encoding="UTF-8"?>
        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                     targetNamespace="http://wfp.com/test">
          <process id="notificationFlow" isExecutable="true">
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
    private String taskId;

    @BeforeEach
    void startProcess() {
        deploymentId = repositoryService.createDeployment()
                .addString("notification-flow.bpmn20.xml", BPMN)
                .tenantId("tenant-notify")
                .deploy()
                .getId();
        String processInstanceId = runtimeService
                .startProcessInstanceByKeyAndTenantId("notificationFlow", "tenant-notify").getId();
        taskId = flowableTaskService.createTaskQuery().processInstanceId(processInstanceId).singleResult().getId();
    }

    @AfterEach
    void deleteDeployment() {
        repositoryService.deleteDeployment(deploymentId, true);
    }

    @Test
    void completingATaskNotifiesTheCompleter() throws Exception {
        perform(post("/api/tasks/{id}/complete", taskId)).andExpect(status().isNoContent());

        perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("TASK_COMPLETED"))
                .andExpect(jsonPath("$.content[0].referenceId").value(taskId));
    }

    @Test
    void completingATaskRecordsAnAuditEntryInTheSameService() throws Exception {
        perform(post("/api/tasks/{id}/complete", taskId)).andExpect(status().isNoContent());

        perform(get("/api/audit").param("entityId", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.eventType == 'task.completed')]").exists());
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(jwt().jwt(j -> j.claim("preferred_username", "notified-user")
                .claim("tenant_id", "tenant-notify"))));
    }
}
