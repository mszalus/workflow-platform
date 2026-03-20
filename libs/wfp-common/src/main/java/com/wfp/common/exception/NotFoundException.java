package com.wfp.common.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String entityName, Object id) {
        super(String.format("%s not found with id: %s", entityName, id));
    }

    public NotFoundException(String message) {
        super(message);
    }
}
