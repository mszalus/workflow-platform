package com.wfp.workflow.engine;

import java.util.List;
import java.util.UUID;

public interface WorkflowEngine {

    List<Violation> validate(String bpmnXml);

    String latestVersion(String tenantId, String workflowKey);

    WorkflowDescriptor describe(String versionId);

    WorkflowRun start(String tenantId, String versionId, UUID itemId);

    void transition(String tenantId, String runId, String transitionId);
}
