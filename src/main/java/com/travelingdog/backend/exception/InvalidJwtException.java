package com.travelingdog.backend.exception;

import org.springframework.security.core.AuthenticationException;

// 유효하지 않은 JWT 예외
public class InvalidJwtException extends AuthenticationException {
    public InvalidJwtException(String message) {
        super(message);
    }
}
