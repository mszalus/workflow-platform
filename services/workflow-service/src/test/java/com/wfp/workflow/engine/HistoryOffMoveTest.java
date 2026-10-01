package com.wfp.workflow.engine;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "flowable.history-level=none")
@ActiveProfiles("test")
class HistoryOffMoveTest {

    private static final String TENANT = "tenant-history-off";

    private static final String VERSION_1 = """
        <?xml version="1.0" encoding="UTF-8"?>
        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" targetNamespace="http://wfp.com/test">
          <process id="historyOffMove" isExecutable="true">
            <startEvent id="start"/>
            <userTask id="open" name="Open"/>
            <userTask id="doing" name="Doing"/>
            <userTask id="done" name="Done"/>
            <endEvent id="end"/>
            <sequenceFlow id="f1" sourceRef="start" targetRef="open"/>
            <sequenceFlow id="f2" sourceRef="open" targetRef="doing"/>
            <sequenceFlow id="f3" sourceRef="doing" targetRef="done"/>
            <sequenceFlow id="f4" sourceRef="done" targetRef="end"/>
          </process>
        </definitions>
        """;

    private static final String VERSION_2 = VERSION_1
            .replace("<userTask id=\"done\" name=\"Done\"/>",
                    "<userTask id=\"review\" name=\"Review\"/><userTask id=\"done\" name=\"Done\"/>")
            .replace("<sequenceFlow id=\"f3\" sourceRef=\"doing\" targetRef=\"done\"/>",
                    "<sequenceFlow id=\"f3\" sourceRef=\"doing\" targetRef=\"review\"/>"
                            + "<sequenceFlow id=\"f5\" sourceRef=\"review\" targetRef=\"done\"/>");

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    private final List<String> deploymentIds = new ArrayList<>();

    @AfterEach
    void deleteDeployments() {
        deploymentIds.forEach(id -> repositoryService.deleteDeployment(id, true));
    }

    @Test
    void movesARunToAStatusOfANewVersionWithHistoryOff() {
        assertThat(((ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration()).getHistoryLevel())
                .isEqualTo(HistoryLevel.NONE);
        deploy(VERSION_1);
        ProcessInstance oldRun = runtimeService.startProcessInstanceByKeyAndTenantId("historyOffMove", TENANT);
        taskService.complete(activeTaskId(oldRun.getId()));
        deploy(VERSION_2);

        runtimeService.deleteProcessInstance(oldRun.getId(), "WORKFLOW_UPGRADE");
        ProcessInstance newRun = runtimeService.startProcessInstanceByKeyAndTenantId("historyOffMove", TENANT);
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(newRun.getId())
                .moveActivityIdTo("open", "doing")
                .changeState();

        assertThat(repositoryService.getProcessDefinition(newRun.getProcessDefinitionId()).getVersion()).isEqualTo(2);
        assertThat(taskService.createTaskQuery().processInstanceId(newRun.getId()).singleResult().getTaskDefinitionKey())
                .isEqualTo("doing");
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(oldRun.getId()).count()).isZero();
    }

    private void deploy(String bpmnXml) {
        deploymentIds.add(repositoryService.createDeployment()
                .addString("history-off-move.bpmn20.xml", bpmnXml)
                .tenantId(TENANT)
                .deploy()
                .getId());
    }

    private String activeTaskId(String processInstanceId) {
        return taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult().getId();
    }
}
