package com.wfp.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Map;

@Data
public class SaveFieldValuesRequest {
    @NotBlank private String processInstanceId;
    @NotBlank private String processDefinitionKey;
    @NotEmpty private Map<String, String> values;
}
