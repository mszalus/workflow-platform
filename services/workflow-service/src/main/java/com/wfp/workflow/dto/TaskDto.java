package com.wfp.workflow.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class TaskDto {
    private String id;
    private String name;
    private String description;
    private String assignee;
    private String owner;
    private String processInstanceId;
    private String processDefinitionId;
    private String taskDefinitionKey;
    private Instant createTime;
    private Instant dueDate;
    private int priority;
    private String tenantId;
    private String formKey;
}
