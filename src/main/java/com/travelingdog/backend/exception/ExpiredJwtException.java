package com.travelingdog.backend.exception;

import org.springframework.security.core.AuthenticationException;

// 만료된 JWT 예외
public class ExpiredJwtException extends AuthenticationException {
    public ExpiredJwtException(String message) {
        super(message);
    }
}
