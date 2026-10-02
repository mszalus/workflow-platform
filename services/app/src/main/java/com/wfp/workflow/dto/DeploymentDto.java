package com.wfp.workflow.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeploymentDto {
    private String deploymentId;
    private String name;
}
