package com.workflowplatform.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDto {

    private String id;
    private String name;
    private String description;
    private String assignee;
    private String owner;
    private String processInstanceId;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String executionId;
    private String formKey;
    private String category;
    private String tenantId;
    private int priority;
    private String status;
    private boolean suspended;
    private Instant createTime;
    private Instant claimTime;
    private Instant dueDate;
    private Map<String, Object> variables;
}
