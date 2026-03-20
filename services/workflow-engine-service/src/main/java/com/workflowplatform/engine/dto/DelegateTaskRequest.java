package com.workflowplatform.engine.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelegateTaskRequest {

    @NotBlank(message = "userId is required")
    private String userId;
}
