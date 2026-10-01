package com.wfp.workflow.engine;

public record Violation(String elementId, int rule, String message) {
}
