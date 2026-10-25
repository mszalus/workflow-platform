package com.wfp.workflow.engine;

import com.wfp.common.exception.BadRequestException;

import java.util.List;
import java.util.Map;

public class InvalidWorkflowException extends BadRequestException {

    public InvalidWorkflowException(List<Violation> violations) {
        super("The workflow breaks " + violations.size() + " tracker rule(s)", Map.of("violations", violations));
    }
}
