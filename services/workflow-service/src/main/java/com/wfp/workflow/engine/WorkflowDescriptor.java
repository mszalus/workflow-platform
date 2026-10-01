package com.wfp.workflow.engine;

import java.util.List;

public record WorkflowDescriptor(
        String initialStatusId,
        List<Status> statuses,
        List<Transition> anyStatusTransitions) {
}
