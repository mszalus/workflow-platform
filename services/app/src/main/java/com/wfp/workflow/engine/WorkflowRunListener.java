package com.wfp.workflow.engine;

import java.util.UUID;

public interface WorkflowRunListener {

    void statusEntered(String tenantId, UUID itemId, String statusId);

    void runEnded(String tenantId, UUID itemId, String endName);
}
