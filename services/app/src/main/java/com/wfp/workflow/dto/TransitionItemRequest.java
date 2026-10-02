package com.wfp.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TransitionItemRequest {
    @NotBlank private String transitionId;
    private String reason;
}
