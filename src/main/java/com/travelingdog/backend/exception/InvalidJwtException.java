package com.travelingdog.backend.exception;

// 유효하지 않은 JWT 예외
public class InvalidJwtException extends RuntimeException {
    public InvalidJwtException(String message) {
        super(message);
    }
}
