package com.hdbank.common.exception;

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String resource, String id) {
        super("RESOURCE_NOT_FOUND", resource + " not found with id: " + id);
    }
}
