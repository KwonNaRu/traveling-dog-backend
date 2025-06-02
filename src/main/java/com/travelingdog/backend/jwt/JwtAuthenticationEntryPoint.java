package com.travelingdog.backend.jwt;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelingdog.backend.dto.ErrorResponse;
import com.travelingdog.backend.exception.ExpiredJwtException;
import com.travelingdog.backend.exception.InvalidJwtException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) {

        try {
            // 원본 예외 확인
            Throwable cause = authException.getCause();
            String errorCode = "UNAUTHORIZED";
            String message = "인증이 필요합니다";

            // 예외 유형에 따른 응답 설정
            if (cause instanceof ExpiredJwtException) {
                errorCode = "EXPIRED_JWT";
                message = "토큰이 만료되었습니다";
            } else if (cause instanceof InvalidJwtException) {
                errorCode = "INVALID_JWT";
                message = "유효하지 않은 토큰입니다";
            }

            // 응답 설정
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");

            // 에러 응답 생성
            Map<String, String> errors = Map.of("error", authException.getMessage());
            ErrorResponse errorResponse = ErrorResponse.of(errorCode, message, errors);

            // JSON 응답 반환
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        } catch (IOException e) {
            throw new RuntimeException("응답 쓰기 중 오류 발생", e);
        }
    }
}