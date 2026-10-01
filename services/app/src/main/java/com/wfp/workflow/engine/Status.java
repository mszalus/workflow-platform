package com.wfp.workflow.engine;

import java.util.List;

public record Status(
        String id,
        String name,
        StatusCategory category,
        List<String> candidateGroups,
        List<Transition> transitions) {
}
