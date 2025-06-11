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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) {

        try {
            log.debug("인증 실패 처리: {}", authException.getMessage());

            String errorCode = "UNAUTHORIZED";
            String message = "인증이 필요합니다";

            // 직접 예외 인스턴스 확인 (authException 자체가 해당 타입인 경우)
            if (authException instanceof ExpiredJwtException) {
                errorCode = "EXPIRED_JWT";
                message = "토큰이 만료되었습니다";
                log.debug("만료된 JWT 토큰: {}", authException.getMessage());
            } else if (authException instanceof InvalidJwtException) {
                errorCode = "INVALID_JWT";
                message = "유효하지 않은 토큰입니다";
                log.debug("유효하지 않은 JWT 토큰: {}", authException.getMessage());
            } else {
                // cause 확인 (내부 예외가 원인인 경우)
                Throwable cause = authException.getCause();
                if (cause instanceof ExpiredJwtException) {
                    errorCode = "EXPIRED_JWT";
                    message = "토큰이 만료되었습니다";
                    log.debug("만료된 JWT 토큰(cause): {}", cause.getMessage());
                } else if (cause instanceof InvalidJwtException) {
                    errorCode = "INVALID_JWT";
                    message = "유효하지 않은 토큰입니다";
                    log.debug("유효하지 않은 JWT 토큰(cause): {}", cause.getMessage());
                }
            }

            // 응답 설정
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");

            // 에러 응답 생성 - 상세 오류 메시지 포함
            Map<String, String> errors = Map.of("error", authException.getMessage());
            ErrorResponse errorResponse = ErrorResponse.of(errorCode, message, errors);

            // JSON 응답 반환
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            log.debug("인증 오류 응답 생성: {}", errorResponse);
        } catch (IOException e) {
            log.error("인증 오류 응답 생성 중 예외 발생", e);
            throw new RuntimeException("응답 쓰기 중 오류 발생", e);
        }
    }
}