package com.sharedexpenses.common;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String resourceType, Long id) {
        return new ResourceNotFoundException(resourceType + " with id " + id + " not found");
    }
}
