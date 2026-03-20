package com.workflowplatform.scheduler.dto;

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
public class ScheduledProcessDto {

    private UUID id;
    private String tenantId;
    private String name;
    private String description;
    private String processDefKey;
    private String cronExpression;
    private Map<String, Object> variables;
    private boolean enabled;
    private String quartzGroup;
    private String quartzJobName;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastFiredAt;
    private Instant nextFireAt;
}
