package com.wfp.workflow.engine.flowable;

import com.wfp.common.exception.BadRequestException;
import com.wfp.common.exception.NotFoundException;
import com.wfp.security.context.TenantContext;
import com.wfp.workflow.engine.Samples;
import com.wfp.workflow.engine.WorkflowEngine;
import com.wfp.workflow.engine.WorkflowRun;
import com.wfp.workflow.service.ItemService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class FlowableWorkflowEngineTest {

    private static final String TENANT = "tenant-engine";

    @Autowired
    private WorkflowEngine engine;

    @Autowired
    private DeploymentService deploymentService;

    @Autowired
    private RepositoryService repositoryService;

    @MockitoBean
    private ItemService listener;

    private final UUID itemId = UUID.randomUUID();

    @AfterEach
    void deleteDeployments() {
        repositoryService.createDeploymentQuery().deploymentTenantId(TENANT).list()
                .forEach(deployment -> repositoryService.deleteDeployment(deployment.getId(), true));
    }

    @Test
    void movesThroughGatewayAndSingleFlowTransitions() {
        WorkflowRun run = start("bug-flow", "bugFlow");
        verify(listener).statusEntered(TENANT, itemId, "open");

        engine.transition(TENANT, run.runId(), "accept");
        verify(listener).statusEntered(TENANT, itemId, "ready");

        engine.transition(TENANT, run.runId(), "startWork");
        verify(listener).statusEntered(TENANT, itemId, "doing");
    }

    @Test
    void endsTheRunAtAnEndEvent() {
        WorkflowRun run = start("bug-flow", "bugFlow");

        engine.transition(TENANT, run.runId(), "reject");

        verify(listener).runEnded(TENANT, itemId, "Rejected");
    }

    @Test
    void anyStatusTransitionTriggersTheEventSubprocess() {
        WorkflowRun run = start("cancel-anywhere", "cancelAnywhere");

        engine.transition(TENANT, run.runId(), "cancelStart");

        verify(listener).runEnded(TENANT, itemId, "Cancelled");
    }

    @Test
    void refusesATransitionTheCurrentStatusDoesNotOffer() {
        WorkflowRun run = start("bug-flow", "bugFlow");

        assertThatThrownBy(() -> engine.transition(TENANT, run.runId(), "startWork"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Open");
    }

    @Test
    void anotherTenantCannotTransitionTheRun() {
        WorkflowRun run = start("bug-flow", "bugFlow");

        assertThatThrownBy(() -> engine.transition("tenant-other", run.runId(), "accept"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deploySetsConditionsOnlyOnTransitionsLeavingAStatusGateway() throws IOException {
        String staleCondition =
                "<sequenceFlow id=\"accept\" name=\"Accept\" sourceRef=\"openChoice\" targetRef=\"ready\">"
                + "<conditionExpression>${transition == 'renamed'}</conditionExpression></sequenceFlow>";
        String bpmnXml = Samples.xml("valid/bug-flow").replace(
                "<sequenceFlow id=\"accept\" name=\"Accept\" sourceRef=\"openChoice\" targetRef=\"ready\"/>",
                staleCondition);
        TenantContext.runAs(TENANT, () -> deploymentService.deploy("bug-flow", null, bpmnXml));
        WorkflowRun run = engine.start(TENANT, engine.latestVersion(TENANT, "bugFlow"), itemId);
        ProcessDefinition version = repositoryService.getProcessDefinition(run.versionId());

        String deployedXml;
        try (InputStream xml = repositoryService.getResourceAsStream(version.getDeploymentId(),
                version.getResourceName())) {
            deployedXml = new String(xml.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(deployedXml)
                .contains("${transition == 'accept'}", "${transition == 'reject'}", "${transition == 'close'}")
                .doesNotContain("${transition == 'startWork'}", "${transition == 'toOpen'}",
                        "${transition == 'renamed'}")
                .contains("${severity == 'High'}");
    }

    private WorkflowRun start(String sample, String workflowKey) {
        TenantContext.runAs(TENANT, () -> deploymentService.deploy(sample, null, Samples.xml("valid/" + sample)));
        return engine.start(TENANT, engine.latestVersion(TENANT, workflowKey), itemId);
    }
}
