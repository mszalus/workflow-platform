package com.wfp.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DelegateTaskRequest {
    @NotBlank private String delegateToUserId;
    private String comment;
}
