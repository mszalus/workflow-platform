package com.wfp.workflow.engine;

public record Flow(String id, String name, String sourceId, String targetId) {
}
