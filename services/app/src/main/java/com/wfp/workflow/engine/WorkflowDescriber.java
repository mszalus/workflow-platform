package com.wfp.workflow.engine;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class WorkflowDescriber {

    private final TrackerProfileValidator validator = new TrackerProfileValidator();

    public WorkflowDescriptor describe(WorkflowGraph graph) {
        List<Violation> violations = validator.validate(graph);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Not a valid tracker workflow: " + violations);
        }
        String initialStatusId = graph.statusesAndEndsReachedFrom(graph.topLevelStartEvents().getFirst().id())
                .iterator().next();
        List<Status> statuses = graph.statuses().stream()
                .map(status -> describeStatus(graph, status))
                .toList();
        List<Transition> anyStatusTransitions = graph.topLevelEventSubprocessStartEvents().stream()
                .map(start -> anyStatusTransition(graph, start))
                .toList();
        return new WorkflowDescriptor(initialStatusId, statuses, anyStatusTransitions);
    }

    private Status describeStatus(WorkflowGraph graph, Node status) {
        StatusCategory category = StatusCategory.valueOf(status.statusCategory());
        if (status.type() == NodeType.SUBPROCESS) {
            return new Status(status.id(), status.name(), category, status.candidateGroups(), List.of(),
                    stepsOf(graph, status));
        }
        return new Status(status.id(), status.name(), category, status.candidateGroups(),
                transitionsFrom(graph, status), List.of());
    }

    private List<Step> stepsOf(WorkflowGraph graph, Node statusSubprocess) {
        return graph.stepsOf(statusSubprocess).stream()
                .map(step -> new Step(step.id(), firstNonBlank(step.name(), step.id()), step.candidateGroups()))
                .toList();
    }

    private List<Transition> transitionsFrom(WorkflowGraph graph, Node status) {
        return graph.outgoing(status.id()).stream()
                .flatMap(leaving -> {
                    Node target = graph.node(leaving.targetId()).orElseThrow();
                    if (target.type() == NodeType.EXCLUSIVE_GATEWAY) {
                        return graph.outgoing(target.id()).stream().map(flow -> transition(graph, flow));
                    }
                    return Stream.of(transition(graph, leaving));
                })
                .toList();
    }

    private Transition transition(WorkflowGraph graph, Flow flow) {
        String targetName = graph.node(flow.targetId()).map(Node::name).orElse(null);
        return new Transition(flow.id(), firstNonBlank(flow.name(), targetName, flow.id()),
                List.copyOf(graph.statusesAndEndsReachedFrom(flow.targetId())));
    }

    private Transition anyStatusTransition(WorkflowGraph graph, Node start) {
        Node eventSubprocess = graph.node(start.parentId()).orElseThrow();
        return new Transition(start.id(), firstNonBlank(eventSubprocess.name(), start.name(), eventSubprocess.id()),
                List.copyOf(graph.statusesAndEndsReachedFrom(start.id())));
    }

    private static String firstNonBlank(String... candidates) {
        return Stream.of(candidates).filter(Objects::nonNull).filter(name -> !name.isBlank()).findFirst().orElseThrow();
    }
}
