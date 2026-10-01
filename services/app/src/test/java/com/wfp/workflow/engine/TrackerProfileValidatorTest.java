package com.wfp.workflow.engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static com.wfp.workflow.engine.Samples.graph;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class TrackerProfileValidatorTest {

    private final TrackerProfileValidator validator = new TrackerProfileValidator();

    @ParameterizedTest
    @ValueSource(strings = {"simple", "bug-flow", "approval-subprocess", "cancel-anywhere", "sla-timer",
            "plain-subprocess-timer", "nested-event-subprocess"})
    void acceptsValidWorkflows(String sample) {
        assertThat(validator.validate(graph("valid/" + sample))).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("invalidWorkflows")
    void reportsEachRuleViolation(String sample, List<String> elementIds, int rule) {
        assertThat(validator.validate(graph("invalid/" + sample)))
                .extracting(Violation::elementId, Violation::rule)
                .containsExactlyInAnyOrderElementsOf(elementIds.stream().map(id -> tuple(id, rule)).toList());
    }

    static Stream<Arguments> invalidWorkflows() {
        return Stream.of(
                Arguments.of("rule1-script-task", List.of("calculate"), 1),
                Arguments.of("rule1-parallel-outside-subprocess", List.of("fork", "join"), 1),
                Arguments.of("rule1-non-interrupting-timer", List.of("reminder"), 1),
                Arguments.of("rule1-non-interrupting-event-subprocess", List.of("note"), 1),
                Arguments.of("rule1-timer-event-subprocess-with-task", List.of("nightly"), 1),
                Arguments.of("rule2-two-start-events", List.of("startA", "startB"), 2),
                Arguments.of("rule2-ambiguous-initial-status", List.of("start"), 2),
                Arguments.of("rule3-missing-category", List.of("doing"), 3),
                Arguments.of("rule3-unknown-category", List.of("doing"), 3),
                Arguments.of("rule3-duplicate-status-name", List.of("doingAgain"), 3),
                Arguments.of("rule3-no-done-status", List.of("noDone"), 3),
                Arguments.of("rule4-unnamed-transition", List.of("skip"), 4),
                Arguments.of("rule4-duplicate-transition-name", List.of("skip"), 4),
                Arguments.of("rule4-shared-gateway", List.of("skip"), 4),
                Arguments.of("rule5-unreachable-status", List.of("orphan"), 5),
                Arguments.of("rule5-no-way-to-end", List.of("ping", "pong"), 5),
                Arguments.of("rule6-two-outgoing-flows", List.of("open"), 6));
    }

    @Test
    void reportsViolationsForARealNonTrackerWorkflowWithoutFailing() {
        List<Violation> violations = validator.validate(graph("invalid/review-sales-lead"));

        violations.forEach(violation -> System.out.println("review-sales-lead: " + violation));
        assertThat(violations).extracting(Violation::rule).contains(3);
    }
}
