package com.wfp.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeployProcessRequest {
    @NotBlank private String name;
    private String category;
    @NotBlank private String bpmnXml;
}
