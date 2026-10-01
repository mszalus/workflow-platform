package com.wfp.workflow.engine;

import java.util.Arrays;

public enum StatusCategory {
    OPEN, TODO, IN_PROGRESS, DONE;

    public static boolean isValid(String value) {
        return Arrays.stream(values()).anyMatch(category -> category.name().equals(value));
    }
}
