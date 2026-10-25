package com.wfp.workflow.engine;

import java.util.List;

public record Step(String id, String name, List<String> candidateGroups) {
}
