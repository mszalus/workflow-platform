package com.wfp.workflow.dto;

import lombok.Data;

import java.util.Map;

@Data
public class CompleteTaskRequest {
    private Map<String, Object> variables;
    private String comment;
}
