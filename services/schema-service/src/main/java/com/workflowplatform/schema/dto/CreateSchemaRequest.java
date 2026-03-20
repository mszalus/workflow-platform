package com.workflowplatform.schema.dto;

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
public class CreateSchemaRequest {

    @NotBlank
    private String processDefKey;

    private String description;

    @Valid
    @NotEmpty
    private List<FieldDefinitionDto> fields;
}
