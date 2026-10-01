package com.wfp.workflow;

import com.wfp.events.TaskCompletedEvent;
import com.wfp.workflow.service.NotificationService;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationRollbackTest {

    private static final String BPMN = """
        <?xml version="1.0" encoding="UTF-8"?>
        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                     targetNamespace="http://wfp.com/test">
          <process id="notificationRollback" isExecutable="true">
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

    @MockitoSpyBean
    private NotificationService notificationService;

    private String deploymentId;
    private String taskId;

    @BeforeEach
    void startProcess() {
        deploymentId = repositoryService.createDeployment()
                .addString("notification-rollback.bpmn20.xml", BPMN)
                .tenantId("tenant-rollback")
                .deploy()
                .getId();
        String processInstanceId = runtimeService
                .startProcessInstanceByKeyAndTenantId("notificationRollback", "tenant-rollback").getId();
        taskId = flowableTaskService.createTaskQuery().processInstanceId(processInstanceId).singleResult().getId();
    }

    @AfterEach
    void deleteDeployment() {
        repositoryService.deleteDeployment(deploymentId, true);
    }

    @Test
    void aFailedNotificationRollsBackTheTaskCompletion() throws Exception {
        doThrow(new IllegalStateException("notification store down"))
                .when(notificationService).notify(any(TaskCompletedEvent.class));

        mockMvc.perform(post("/api/tasks/{id}/complete", taskId)
                        .with(jwt().jwt(j -> j.claim("preferred_username", "user-rollback")
                                .claim("tenant_id", "tenant-rollback"))))
                .andExpect(status().is5xxServerError());

        assertThat(flowableTaskService.createTaskQuery().taskId(taskId).count()).isEqualTo(1);
    }
}
