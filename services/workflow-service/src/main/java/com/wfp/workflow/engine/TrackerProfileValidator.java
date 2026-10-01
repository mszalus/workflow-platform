package com.wfp.workflow.engine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

public class TrackerProfileValidator {

    public List<Violation> validate(WorkflowGraph graph) {
        List<Violation> violations = new ArrayList<>();
        checkAllowedElements(graph, violations);
        checkStartEvent(graph, violations);
        checkStatuses(graph, violations);
        checkTransitionNames(graph, violations);
        checkReachability(graph, violations);
        checkSingleOutgoingFlow(graph, violations);
        return violations;
    }

    private void checkAllowedElements(WorkflowGraph graph, List<Violation> violations) {
        for (Node node : graph.nodes()) {
            if (node.type() == NodeType.UNSUPPORTED) {
                violations.add(new Violation(node.id(), 1,
                        "A " + node.elementType() + " is not allowed in a tracker workflow"));
            } else if (node.type() == NodeType.PARALLEL_GATEWAY && !graph.insideStatusSubprocess(node)) {
                violations.add(new Violation(node.id(), 1,
                        "A parallel gateway is only allowed inside a status subprocess"));
            }
        }
    }

    private void checkStartEvent(WorkflowGraph graph, List<Violation> violations) {
        List<Node> starts = graph.topLevelStartEvents();
        if (starts.size() != 1) {
            String message = "A tracker workflow needs exactly one start event, found " + starts.size();
            if (starts.isEmpty()) {
                violations.add(new Violation(graph.processId(), 2, message));
            }
            starts.forEach(start -> violations.add(new Violation(start.id(), 2, message)));
            return;
        }
        Node start = starts.getFirst();
        Set<String> reached = graph.statusesAndEndsReachedFrom(start.id());
        if (reached.size() != 1 || reached.contains(WorkflowGraph.END)) {
            violations.add(new Violation(start.id(), 2,
                    "The start event must lead to exactly one status, which becomes the initial status"));
        }
    }

    private void checkStatuses(WorkflowGraph graph, List<Violation> violations) {
        Set<String> names = new HashSet<>();
        boolean hasDone = false;
        for (Node status : graph.statuses()) {
            if (status.statusCategory() == null) {
                violations.add(new Violation(status.id(), 3,
                        "Status " + label(status) + " needs a wfp:statusCategory"));
            } else if (!StatusCategory.isValid(status.statusCategory())) {
                violations.add(new Violation(status.id(), 3,
                        "Status " + label(status) + " has an unknown category " + status.statusCategory()
                                + "; use OPEN, TODO, IN_PROGRESS or DONE"));
            }
            hasDone |= StatusCategory.DONE.name().equals(status.statusCategory());
            if (isBlank(status.name())) {
                violations.add(new Violation(status.id(), 3, "Status " + status.id() + " needs a name"));
            } else if (!names.add(status.name().trim().toLowerCase(Locale.ROOT))) {
                violations.add(new Violation(status.id(), 3,
                        "Status name " + label(status) + " is used more than once"));
            }
        }
        if (!hasDone) {
            violations.add(new Violation(graph.processId(), 3, "At least one status needs the category DONE"));
        }
    }

    private void checkTransitionNames(WorkflowGraph graph, List<Violation> violations) {
        for (Node status : graph.statuses()) {
            for (Flow leaving : graph.outgoing(status.id())) {
                graph.node(leaving.targetId())
                        .filter(target -> target.type() == NodeType.EXCLUSIVE_GATEWAY)
                        .ifPresent(gateway -> checkGatewayFlowNames(graph, gateway, violations));
            }
        }
    }

    private void checkGatewayFlowNames(WorkflowGraph graph, Node gateway, List<Violation> violations) {
        Set<String> names = new HashSet<>();
        for (Flow transition : graph.outgoing(gateway.id())) {
            if (isBlank(transition.name())) {
                violations.add(new Violation(transition.id(), 4, "A transition needs a name"));
            } else if (!names.add(transition.name().trim().toLowerCase(Locale.ROOT))) {
                violations.add(new Violation(transition.id(), 4,
                        "Transition name '" + transition.name() + "' is used more than once on this gateway"));
            }
        }
    }

    private void checkReachability(WorkflowGraph graph, List<Violation> violations) {
        List<String> entryPoints = Stream.concat(graph.topLevelStartEvents().stream(),
                graph.eventSubprocessStartEvents().stream()).map(Node::id).toList();
        Set<String> reachableFromStart = graph.nodesReachableFrom(entryPoints);
        for (Node status : graph.statuses()) {
            if (!reachableFromStart.contains(status.id())) {
                violations.add(new Violation(status.id(), 5,
                        "Status " + label(status) + " can't be reached from the start event"));
            }
            boolean endReachable = graph.nodesReachableFrom(graph.successors(status)).stream()
                    .flatMap(id -> graph.node(id).stream())
                    .anyMatch(graph::isTerminalEnd);
            if (!endReachable) {
                violations.add(new Violation(status.id(), 5,
                        "No end event can be reached from status " + label(status)));
            }
        }
    }

    private void checkSingleOutgoingFlow(WorkflowGraph graph, List<Violation> violations) {
        for (Node status : graph.statuses()) {
            int leaving = graph.outgoing(status.id()).size();
            if (leaving != 1) {
                violations.add(new Violation(status.id(), 6,
                        "Status " + label(status) + " needs exactly one outgoing flow, found " + leaving));
            }
        }
    }

    private static String label(Node node) {
        return isBlank(node.name()) ? node.id() : "'" + node.name() + "'";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
