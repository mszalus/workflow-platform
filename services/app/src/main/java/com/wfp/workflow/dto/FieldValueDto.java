package com.wfp.workflow.dto;

import com.wfp.workflow.entity.FieldType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FieldValueDto {
    private String fieldSchemaId;
    private String fieldKey;
    private String label;
    private FieldType fieldType;
    private String value;
}
