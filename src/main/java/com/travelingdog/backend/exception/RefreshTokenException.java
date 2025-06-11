package com.travelingdog.backend.exception;

// Refresh Token 만료 or 위조
public class RefreshTokenException extends RuntimeException {
    public RefreshTokenException(String message) {
        super(message);
    }
}
