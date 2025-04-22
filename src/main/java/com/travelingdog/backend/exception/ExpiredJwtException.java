package com.travelingdog.backend.exception;

// 만료된 JWT 예외
public class ExpiredJwtException extends RuntimeException {
    public ExpiredJwtException(String message) {
        super(message);
    }
}
