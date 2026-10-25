package com.wfp.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompleteStepRequest {
    @NotBlank @Size(max = 255) private String stepId;
    @Size(max = 255) private String reason;
}
