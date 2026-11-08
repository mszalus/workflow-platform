package com.wfp.workflow.dto;

import com.wfp.common.exception.BadRequestException;

import java.util.List;
import java.util.regex.Pattern;

public record ItemFilter(String field, Operator operator, List<String> values) {

    public enum Operator {
        EQ, NE, IN, GTE, LTE
    }

    private static final String CUSTOM_FIELD_PREFIX = "cf.";
    private static final String ESCAPED_COMMA = "\\,";
    private static final Pattern UNESCAPED_COMMA = Pattern.compile("(?<!\\\\),");

    public static ItemFilter parse(String raw) {
        String[] parts = raw.split(":", 3);
        if (parts.length != 3 || parts[0].isBlank() || parts[2].isBlank()) {
            throw new BadRequestException("Filter '" + raw + "' is not field:operator:value");
        }
        Operator operator = operator(parts[1], raw);
        List<String> values = operator == Operator.IN ? commaSeparated(parts[2]) : List.of(parts[2].trim());
        return new ItemFilter(parts[0].trim(), operator, values);
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

    private static List<String> commaSeparated(String raw) {
        return UNESCAPED_COMMA.splitAsStream(raw)
                .map(value -> value.replace(ESCAPED_COMMA, ",").trim())
                .filter(value -> !value.isEmpty())
                .toList();
    }
}
