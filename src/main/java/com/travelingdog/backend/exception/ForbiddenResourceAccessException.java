package com.travelingdog.backend.exception;

public class ForbiddenResourceAccessException extends RuntimeException {
    public ForbiddenResourceAccessException() {
        super("금지된 리소스 접근");
    }

    public ForbiddenResourceAccessException(String message) {
        super(message);
    }
}
