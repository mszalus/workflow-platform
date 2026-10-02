package com.wfp.workflow.dto;

import com.wfp.workflow.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class CreateItemRequest {
    @NotBlank private String project;
    @NotBlank private String type;
    @NotBlank private String title;
    private String description;
    private Priority priority = Priority.MEDIUM;
    private String assignee;
    private Map<String, Object> fields = new LinkedHashMap<>();
}
