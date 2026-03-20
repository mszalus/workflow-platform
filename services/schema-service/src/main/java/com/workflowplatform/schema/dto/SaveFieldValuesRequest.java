package com.workflowplatform.schema.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveFieldValuesRequest {

    @NotBlank
    private String processInstanceId;

    private String taskId;

    @Valid
    @NotEmpty
    private List<FieldValueDto> values;
}
