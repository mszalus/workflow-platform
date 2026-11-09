package com.wfp.workflow.dto;

import com.wfp.workflow.dto.ProjectDto.ItemTypeDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateProjectRequest {
    @NotBlank @Size(max = 255) private String name;
    @NotEmpty private List<@Valid ItemTypeDto> itemTypes;
}
