package com.wfp.customfields.dto;

import com.wfp.customfields.entity.FieldType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FieldSchemaDto {
    private String id;
    private String processDefinitionKey;
    private String fieldKey;
    private String label;
    private FieldType fieldType;
    private boolean required;
    private int sortOrder;
    private String defaultValue;
    private String placeholder;
    private String validationRegex;
    private List<OptionDto> options;

    @Data
    @Builder
    public static class OptionDto {
        private String id;
        private String label;
        private String value;
        private int sortOrder;
    }
}
