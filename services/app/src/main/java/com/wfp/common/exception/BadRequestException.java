package com.wfp.common.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class BadRequestException extends RuntimeException {

    private final Map<String, Object> details;

    public BadRequestException(String message) {
        this(message, null);
    }

    public BadRequestException(String message, Map<String, Object> details) {
        super(message);
        this.details = details;
    }
}
