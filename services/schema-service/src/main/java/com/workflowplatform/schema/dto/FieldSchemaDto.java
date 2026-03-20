package com.workflowplatform.schema.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldSchemaDto {

    private UUID id;
    private String tenantId;

    @NotBlank
    private String processDefKey;

    private int version;
    private boolean active;
    private String description;

    @Valid
    private List<FieldDefinitionDto> fields;

    private Instant createdAt;
    private Instant updatedAt;
}
