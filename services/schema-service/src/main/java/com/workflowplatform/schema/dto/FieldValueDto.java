package com.workflowplatform.schema.dto;

import com.workflowplatform.schema.domain.FieldType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldValueDto {

    private UUID id;
    private String tenantId;

    @NotBlank
    private String processInstanceId;

    private String taskId;

    @NotBlank
    private String fieldKey;

    @NotNull
    private FieldType fieldType;

    // Only one of these will be non-null depending on fieldType
    private String valueText;
    private BigDecimal valueNumber;
    private LocalDate valueDate;
    private Boolean valueBoolean;
    private String valueJson;

    private Instant createdAt;
    private Instant updatedAt;
}
