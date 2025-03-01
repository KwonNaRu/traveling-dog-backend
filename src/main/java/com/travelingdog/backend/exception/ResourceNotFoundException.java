package com.travelingdog.backend.exception;

public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String message;

    public ResourceNotFoundException(String resourceName, String message) {
        super(message);
        this.resourceName = resourceName;
        this.message = message;
    }

    public String getResourceName() {
        return resourceName;
    }

    @Override
    public String getMessage() {
        return message;
    }
}