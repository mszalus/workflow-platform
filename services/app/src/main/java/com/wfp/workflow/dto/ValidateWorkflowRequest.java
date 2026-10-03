package com.wfp.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ValidateWorkflowRequest {
    @NotBlank private String bpmnXml;
}
