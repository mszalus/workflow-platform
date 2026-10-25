package com.wfp.workflow.dto;

import com.wfp.common.exception.BadRequestException;

import java.util.Arrays;
import java.util.List;

public record ItemFilter(String field, Operator operator, List<String> values) {

    public enum Operator {
        EQ, NE, IN, GTE, LTE
    }

    private static final String CUSTOM_FIELD_PREFIX = "cf.";

    public static ItemFilter parse(String raw) {
        String[] parts = raw.split(":", 3);
        if (parts.length != 3 || parts[0].isBlank() || parts[2].isBlank()) {
            throw new BadRequestException("Filter '" + raw + "' is not field:operator:value");
        }
        return new ItemFilter(parts[0].trim(), operator(parts[1], raw), values(parts[2]));
    }

    public boolean isCustomField() {
        return field.startsWith(CUSTOM_FIELD_PREFIX);
    }

    public String customFieldKey() {
        return field.substring(CUSTOM_FIELD_PREFIX.length());
    }

    public String value() {
        return values.getFirst();
    }

    private static Operator operator(String raw, String filter) {
        try {
            return Operator.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Filter '" + filter + "' uses an unknown operator '" + raw.trim() + "'");
        }
    }

    private static List<String> values(String raw) {
        return Arrays.stream(raw.split(",")).map(String::trim).filter(value -> !value.isEmpty()).toList();
    }
}
