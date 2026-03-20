package com.workflowplatform.engine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    private String eventType;
    private String tenantId;
    private String actorId;

    @Builder.Default
    private Instant occurredAt = Instant.now();

    // Process instance fields
    private String processInstanceId;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String businessKey;

    // Task fields
    private String taskId;
    private String taskName;
    private String assignee;

    // Generic payload for additional data
    private Map<String, Object> payload;

    // Event type constants
    public static final String PROCESS_INSTANCE_STARTED   = "PROCESS_INSTANCE_STARTED";
    public static final String PROCESS_INSTANCE_COMPLETED = "PROCESS_INSTANCE_COMPLETED";
    public static final String PROCESS_INSTANCE_CANCELLED = "PROCESS_INSTANCE_CANCELLED";
    public static final String PROCESS_INSTANCE_SUSPENDED = "PROCESS_INSTANCE_SUSPENDED";
    public static final String PROCESS_INSTANCE_ACTIVATED = "PROCESS_INSTANCE_ACTIVATED";
    public static final String TASK_CREATED               = "TASK_CREATED";
    public static final String TASK_ASSIGNED              = "TASK_ASSIGNED";
    public static final String TASK_CLAIMED               = "TASK_CLAIMED";
    public static final String TASK_COMPLETED             = "TASK_COMPLETED";
    public static final String TASK_DELEGATED             = "TASK_DELEGATED";
    public static final String DEPLOYMENT_CREATED         = "DEPLOYMENT_CREATED";
}
