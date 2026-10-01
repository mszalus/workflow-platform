package com.wfp.workflow.engine;

import java.util.List;
import java.util.stream.Stream;

public class WorkflowDescriber {

    public WorkflowDescriptor describe(WorkflowGraph graph) {
        String initialStatusId = graph.statusesAndEndsReachedFrom(graph.topLevelStartEvents().getFirst().id())
                .iterator().next();
        List<Status> statuses = graph.statuses().stream()
                .map(status -> new Status(status.id(), status.name(), StatusCategory.valueOf(status.statusCategory()),
                        status.candidateGroups(), transitionsFrom(graph, status)))
                .toList();
        List<Transition> anyStatusTransitions = graph.eventSubprocessStartEvents().stream()
                .map(start -> transition(graph, start.id(), anyStatusTransitionName(graph, start), start.id()))
                .toList();
        return new WorkflowDescriptor(initialStatusId, statuses, anyStatusTransitions);
    }

    private List<Transition> transitionsFrom(WorkflowGraph graph, Node status) {
        return graph.outgoing(status.id()).stream()
                .flatMap(leaving -> {
                    Node target = graph.node(leaving.targetId()).orElseThrow();
                    if (target.type() == NodeType.EXCLUSIVE_GATEWAY) {
                        return graph.outgoing(target.id()).stream()
                                .map(flow -> transition(graph, flow.id(), flow.name(), flow.targetId()));
                    }
                    String name = leaving.name() != null ? leaving.name() : target.name();
                    return Stream.of(transition(graph, leaving.id(), name, leaving.targetId()));
                })
                .toList();
    }

    private Transition transition(WorkflowGraph graph, String id, String name, String firstNodeId) {
        return new Transition(id, name, List.copyOf(graph.statusesAndEndsReachedFrom(firstNodeId)));
    }

    private String anyStatusTransitionName(WorkflowGraph graph, Node start) {
        return graph.node(start.parentId()).map(Node::name).filter(name -> !name.isBlank()).orElse(start.name());
    }
}
