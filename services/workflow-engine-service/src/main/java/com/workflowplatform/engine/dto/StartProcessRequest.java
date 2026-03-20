package com.workflowplatform.engine.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartProcessRequest {

    @NotBlank(message = "processDefinitionKey is required")
    private String processDefinitionKey;

    private String businessKey;

    private String name;

    private Map<String, Object> variables;

    /** Optional: pin to a specific process definition version */
    private String processDefinitionId;
}
