package com.hdbank.common.exception;

public class DuplicateResourceException extends BusinessException {
    public DuplicateResourceException(String resource, String field, String value) {
        super("DUPLICATE_RESOURCE", resource + " already exists with " + field + ": " + value);
    }
}
