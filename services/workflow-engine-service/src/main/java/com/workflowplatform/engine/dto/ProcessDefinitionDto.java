package com.workflowplatform.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessDefinitionDto {

    private String id;
    private String key;
    private String name;
    private String description;
    private int version;
    private String category;
    private String deploymentId;
    private String resourceName;
    private String diagramResourceName;
    private boolean suspended;
    private String tenantId;
    private Instant deployedAt;
}
