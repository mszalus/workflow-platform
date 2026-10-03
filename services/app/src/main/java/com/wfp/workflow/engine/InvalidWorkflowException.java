package com.wfp.workflow.engine;

import lombok.Getter;

import java.util.List;

@Getter
public class InvalidWorkflowException extends RuntimeException {

    private final List<Violation> violations;

    public InvalidWorkflowException(List<Violation> violations) {
        super("The workflow breaks " + violations.size() + " tracker rule(s)");
        this.violations = violations;
    }
}
