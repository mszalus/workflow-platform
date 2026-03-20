package com.workflowplatform.schema.dto;

import com.workflowplatform.schema.domain.FieldType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDefinitionDto {

    private UUID id;

    @NotBlank
    private String fieldKey;

    @NotBlank
    private String label;

    @NotNull
    private FieldType fieldType;

    private boolean required;
    private String validationRules;
    private String options;
    private String[] visibleOnTasks;
    private String[] editableByRoles;
    private int displayOrder;
    private String placeholder;
    private String helpText;
}
