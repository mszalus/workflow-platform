package com.wfp.workflow.engine;

import java.util.List;

public record Transition(String id, String name, List<String> targetStatusIds) {
}
