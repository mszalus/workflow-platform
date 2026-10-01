package com.wfp.workflow.engine;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public record WorkflowGraph(String processId, List<Node> nodes, List<Flow> flows) {

    public static final String END = "END";

    public Optional<Node> node(String id) {
        return nodes.stream().filter(node -> node.id().equals(id)).findFirst();
    }

    public List<Flow> outgoing(String nodeId) {
        return flows.stream().filter(flow -> flow.sourceId().equals(nodeId)).toList();
    }

    public List<Node> statuses() {
        return nodes.stream().filter(this::isStatus).toList();
    }

    public boolean isStatus(Node node) {
        if (insideStatusSubprocess(node)) {
            return false;
        }
        return node.type() == NodeType.USER_TASK
                || (node.type() == NodeType.SUBPROCESS && node.statusCategory() != null);
    }

    public boolean insideStatusSubprocess(Node node) {
        Optional<Node> parent = parentOf(node);
        while (parent.isPresent()) {
            if (parent.get().type() == NodeType.SUBPROCESS && parent.get().statusCategory() != null) {
                return true;
            }
            parent = parentOf(parent.get());
        }
        return false;
    }

    public List<Node> topLevelStartEvents() {
        return nodes.stream().filter(node -> node.type() == NodeType.START && node.parentId() == null).toList();
    }

    public List<Node> eventSubprocessStartEvents() {
        return nodes.stream()
                .filter(node -> node.type() == NodeType.START)
                .filter(node -> parentOf(node).map(parent -> parent.type() == NodeType.EVENT_SUBPROCESS).orElse(false))
                .toList();
    }

    public boolean isTerminalEnd(Node node) {
        return node.type() == NodeType.END && successors(node).isEmpty();
    }

    public Set<String> statusesAndEndsReachedFrom(String nodeId) {
        Set<String> reached = new LinkedHashSet<>();
        Set<String> visited = new HashSet<>();
        Deque<String> pending = new ArrayDeque<>(List.of(nodeId));
        while (!pending.isEmpty()) {
            String id = pending.poll();
            if (!visited.add(id)) {
                continue;
            }
            Optional<Node> node = node(id);
            if (node.isEmpty()) {
                continue;
            }
            if (isStatus(node.get())) {
                reached.add(id);
            } else if (isTerminalEnd(node.get())) {
                reached.add(END);
            } else {
                pending.addAll(successors(node.get()));
            }
        }
        return reached;
    }

    public Set<String> nodesReachableFrom(Collection<String> nodeIds) {
        Set<String> visited = new HashSet<>();
        Deque<String> pending = new ArrayDeque<>(nodeIds);
        while (!pending.isEmpty()) {
            String id = pending.poll();
            if (visited.add(id)) {
                node(id).ifPresent(node -> pending.addAll(successors(node)));
            }
        }
        return visited;
    }

    public List<String> successors(Node node) {
        return switch (node.type()) {
            case END -> endsPlainSubprocess(node) ? targetsOf(node.parentId()) : List.of();
            case SUBPROCESS -> isStatus(node) ? leaving(node) : startsInside(node);
            case EVENT_SUBPROCESS -> List.of();
            default -> leaving(node);
        };
    }

    private boolean endsPlainSubprocess(Node end) {
        return parentOf(end).map(parent -> parent.type() == NodeType.SUBPROCESS && !isStatus(parent)).orElse(false);
    }

    private List<String> leaving(Node node) {
        Stream<String> flowTargets = targetsOf(node.id()).stream();
        Stream<String> attachedTimers = nodes.stream()
                .filter(candidate -> candidate.type() == NodeType.BOUNDARY_TIMER)
                .filter(timer -> node.id().equals(timer.attachedToId()))
                .map(Node::id);
        return Stream.concat(flowTargets, attachedTimers).toList();
    }

    private List<String> targetsOf(String nodeId) {
        return outgoing(nodeId).stream().map(Flow::targetId).toList();
    }

    private List<String> startsInside(Node subprocess) {
        return nodes.stream()
                .filter(node -> node.type() == NodeType.START && subprocess.id().equals(node.parentId()))
                .map(Node::id)
                .toList();
    }

    private Optional<Node> parentOf(Node node) {
        return node.parentId() == null ? Optional.empty() : node(node.parentId());
    }
}
