package com.wfp.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TransitionItemRequest {
    @NotBlank @Size(max = 255) private String transitionId;
    @Size(max = 255) private String reason;
}
