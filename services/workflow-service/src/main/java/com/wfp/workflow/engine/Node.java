package com.wfp.workflow.engine;

import java.util.List;

public record Node(
        String id,
        String name,
        NodeType type,
        String elementType,
        String statusCategory,
        String parentId,
        String attachedToId,
        List<String> candidateGroups) {
}
