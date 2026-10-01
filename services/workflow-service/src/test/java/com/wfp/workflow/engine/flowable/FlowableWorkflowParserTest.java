package com.wfp.workflow.engine.flowable;

import com.wfp.workflow.engine.Node;
import com.wfp.workflow.engine.NodeType;
import com.wfp.workflow.engine.WorkflowGraph;
import org.junit.jupiter.api.Test;

import static com.wfp.workflow.engine.Samples.graph;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlowableWorkflowParserTest {

    @Test
    void readsStatusCategoryOnUserTasksAndCandidateGroups() {
        Node open = node(graph("valid/bug-flow"), "open");

        assertThat(open.type()).isEqualTo(NodeType.USER_TASK);
        assertThat(open.statusCategory()).isEqualTo("OPEN");
        assertThat(open.candidateGroups()).containsExactly("triage");
    }

    @Test
    void readsStatusCategoryOnSubprocessesAndNestsTheirChildren() {
        WorkflowGraph graph = graph("valid/approval-subprocess");

        assertThat(node(graph, "inApproval").type()).isEqualTo(NodeType.SUBPROCESS);
        assertThat(node(graph, "inApproval").statusCategory()).isEqualTo("IN_PROGRESS");
        assertThat(node(graph, "legal").parentId()).isEqualTo("inApproval");
        assertThat(node(graph, "fork").type()).isEqualTo(NodeType.PARALLEL_GATEWAY);
        assertThat(graph.flows()).extracting("id").contains("a1", "submit");
    }

    @Test
    void readsBoundaryTimersAndMessageEventSubprocesses() {
        assertThat(node(graph("valid/sla-timer"), "slaBreached"))
                .extracting(Node::type, Node::attachedToId)
                .containsExactly(NodeType.BOUNDARY_TIMER, "open");
        assertThat(node(graph("valid/cancel-anywhere"), "cancel").type()).isEqualTo(NodeType.EVENT_SUBPROCESS);
    }

    @Test
    void marksElementsOutsideTheProfileAsUnsupported() {
        assertThat(node(graph("invalid/rule1-script-task"), "calculate"))
                .extracting(Node::type, Node::elementType)
                .containsExactly(NodeType.UNSUPPORTED, "scriptTask");
    }

    @Test
    void rejectsDocumentTypeDeclarations() {
        String withDoctype = "<?xml version=\"1.0\"?>"
                + "<!DOCTYPE definitions [<!ENTITY x SYSTEM \"file:///etc/hostname\">]>"
                + "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\">&x;</definitions>";

        assertThatThrownBy(() -> new FlowableWorkflowParser().parse(withDoctype))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Node node(WorkflowGraph graph, String id) {
        return graph.node(id).orElseThrow();
    }
}
