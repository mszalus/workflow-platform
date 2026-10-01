package com.wfp.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class StartProcessRequest {
    @NotBlank private String processDefinitionKey;
    private String businessKey;
    private Map<String, Object> variables;
}
