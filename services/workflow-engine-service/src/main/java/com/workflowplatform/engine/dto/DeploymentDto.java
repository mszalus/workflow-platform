package com.workflowplatform.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentDto {

    private String id;
    private String name;
    private String category;
    private String tenantId;
    private Instant deployedAt;
    private List<String> deployedArtifacts;
    private List<ProcessDefinitionDto> processDefinitions;
}
