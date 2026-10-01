package com.wfp.workflow.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.wfp.workflow.engine.Samples.graph;
import static com.wfp.workflow.engine.WorkflowGraph.END;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class WorkflowDescriberTest {

    private final WorkflowDescriber describer = new WorkflowDescriber();

    @Test
    void describesAStraightWorkflow() {
        WorkflowDescriptor descriptor = describer.describe(graph("valid/simple"));

        assertThat(descriptor.initialStatusId()).isEqualTo("open");
        assertThat(descriptor.statuses())
                .extracting(Status::id, Status::name, Status::category)
                .containsExactly(
                        tuple("open", "Open", StatusCategory.OPEN),
                        tuple("doing", "Doing", StatusCategory.IN_PROGRESS),
                        tuple("done", "Done", StatusCategory.DONE));
        assertThat(transitions(descriptor, "open"))
                .containsExactly(new Transition("startWork", "Start work", List.of("doing")));
        assertThat(transitions(descriptor, "done")).containsExactly(new Transition("close", "Close", List.of(END)));
    }

    @Test
    void takesTransitionsFromTheGatewayAfterAStatusAndFollowsAutomaticRouting() {
        WorkflowDescriptor descriptor = describer.describe(graph("valid/bug-flow"));

        assertThat(transitions(descriptor, "open")).containsExactly(
                new Transition("accept", "Accept", List.of("ready")),
                new Transition("reject", "Reject", List.of(END)));
        assertThat(transitions(descriptor, "doing")).containsExactly(
                new Transition("submit", "Submit", List.of("review", "done")),
                new Transition("block", "Block", List.of("ready")));
        assertThat(transitions(descriptor, "done")).containsExactly(
                new Transition("close", "Close", List.of(END)),
                new Transition("reopen", "Reopen", List.of("open")));
        assertThat(status(descriptor, "review").candidateGroups()).containsExactly("reviewers");
    }

    @Test
    void treatsAStatusSubprocessAsOneStatus() {
        WorkflowDescriptor descriptor = describer.describe(graph("valid/approval-subprocess"));

        assertThat(descriptor.statuses()).extracting(Status::id).containsExactly("draft", "inApproval", "approved");
        assertThat(status(descriptor, "inApproval").category()).isEqualTo(StatusCategory.IN_PROGRESS);
        assertThat(transitions(descriptor, "inApproval"))
                .containsExactly(new Transition("complete", "Complete", List.of("approved")));
    }

    @Test
    void listsMessageEventSubprocessesAsTransitionsFromAnyStatus() {
        assertThat(describer.describe(graph("valid/cancel-anywhere")).anyStatusTransitions())
                .containsExactly(new Transition("cancelStart", "Cancel", List.of(END)));
    }

    @Test
    void leavesBoundaryTimersOutOfTheUserTransitions() {
        WorkflowDescriptor descriptor = describer.describe(graph("valid/sla-timer"));

        assertThat(transitions(descriptor, "open")).extracting(Transition::id).containsExactly("startWork");
        assertThat(descriptor.statuses()).extracting(Status::id).contains("escalated");
    }

    private List<Transition> transitions(WorkflowDescriptor descriptor, String statusId) {
        return status(descriptor, statusId).transitions();
    }

    private Status status(WorkflowDescriptor descriptor, String statusId) {
        return descriptor.statuses().stream()
                .filter(status -> status.id().equals(statusId))
                .findFirst()
                .orElseThrow();
    }
}
