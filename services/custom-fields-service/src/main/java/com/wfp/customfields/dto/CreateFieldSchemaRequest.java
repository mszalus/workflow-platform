package com.wfp.customfields.dto;

import com.wfp.customfields.entity.FieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateFieldSchemaRequest {
    @NotBlank private String processDefinitionKey;
    @NotBlank private String fieldKey;
    @NotBlank private String label;
    @NotNull private FieldType fieldType;
    private boolean required;
    private int sortOrder;
    private String defaultValue;
    private String placeholder;
    private String validationRegex;
    private List<OptionDto> options;

    @Data
    public static class OptionDto {
        private String label;
        private String value;
        private int sortOrder;
    }
}
