package com.wfp.workflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDto {
    @NotBlank private String key;
    @NotBlank private String name;
    @NotEmpty private List<@Valid ItemTypeDto> itemTypes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemTypeDto {
        @NotBlank private String name;
        @NotBlank private String workflowKey;
    }
}
