package com.wfp.workflow.engine;

import java.util.List;
import java.util.UUID;

public interface WorkflowEngine {

    List<Violation> validate(String bpmnXml);

    String latestVersion(String tenantId, String workflowKey);

    WorkflowDescriptor describe(String versionId);

    ProposedWorkflow inspect(String bpmnXml);

    WorkflowRun start(String tenantId, String versionId, UUID itemId);

    WorkflowRun startAt(String tenantId, String versionId, UUID itemId, String statusId);

    void cancel(String tenantId, String runId, String reason);

    void transition(String tenantId, String runId, String transitionId);

    void completeStep(String tenantId, String runId, String stepId);

    List<Step> openSteps(String tenantId, String runId);
}
