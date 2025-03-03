package com.travelingdog.backend.handler;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.travelingdog.backend.dto.ErrorResponse;
import com.travelingdog.backend.exception.DuplicateEmailException;
import com.travelingdog.backend.exception.ResourceNotFoundException;
import com.travelingdog.backend.exception.InvalidRequestException;
import com.travelingdog.backend.exception.ExternalApiException;
import com.travelingdog.backend.exception.ForbiddenResourceAccessException;
import com.travelingdog.backend.exception.UnauthorizedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationException(
                        MethodArgumentNotValidException ex) {
                // FieldError 리스트 추출
                List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();

                // 오류 정보를 Map으로 변환
                Map<String, String> errors = fieldErrors.stream()
                                .collect(Collectors.toMap(
                                                FieldError::getField, // 필드명 추출
                                                FieldError::getDefaultMessage // 메시지 추출
                                ));

                return ResponseEntity.badRequest()
                                .body(ErrorResponse.of("VALIDATION_FAILED", "입력값 검증 실패", errors));
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException e) {
                System.out.println("BadCredentialsException 처리: " + e.getMessage());
                Map<String, String> errors = Map.of("credentials",
                                e.getMessage() != null ? e.getMessage() : "인증 정보가 올바르지 않습니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ErrorResponse.of("INVALID_CREDENTIALS", "인증 실패", errors));
        }

        @ExceptionHandler(DuplicateEmailException.class)
        public ResponseEntity<ErrorResponse> handleDuplicateEmailException(DuplicateEmailException e) {
                Map<String, String> errors = Map.of("email", e.getMessage());
                return ResponseEntity.badRequest()
                                .body(ErrorResponse.of("DUPLICATE_EMAIL", "이미 가입된 이메일입니다.", errors));
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException e) {
                Map<String, String> errors = Map.of("resource", e.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ErrorResponse.of("RESOURCE_NOT_FOUND", "리소스를 찾을 수 없습니다.", errors));
        }

        @ExceptionHandler(InvalidRequestException.class)
        public ResponseEntity<ErrorResponse> handleInvalidRequestException(InvalidRequestException e) {
                Map<String, String> errors = Map.of("request", e.getMessage());
                return ResponseEntity.badRequest()
                                .body(ErrorResponse.of("INVALID_REQUEST", "잘못된 요청입니다.", errors));
        }

        @ExceptionHandler(ExternalApiException.class)
        public ResponseEntity<ErrorResponse> handleExternalApiException(ExternalApiException e) {
                Map<String, String> errors = Map.of("api", e.getMessage());
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .body(ErrorResponse.of("EXTERNAL_API_ERROR", "외부 API 오류", errors));
        }

        @ExceptionHandler(MissingRequestHeaderException.class)
        public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(MissingRequestHeaderException e) {
                Map<String, String> errors = Map.of("header", e.getMessage());
                return ResponseEntity.badRequest()
                                .body(ErrorResponse.of("MISSING_HEADER", "필수 헤더가 누락되었습니다.", errors));
        }

        // 인증이 필요한 요청에서 인증이 실패한 경우
        @ExceptionHandler(UnauthorizedException.class)
        public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException e) {
                Map<String, String> errors = Map.of("error", e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ErrorResponse.of("UNAUTHORIZED", "인증이 필요한 요청입니다.", errors));
        }

        @ExceptionHandler(ForbiddenResourceAccessException.class)
        public ResponseEntity<ErrorResponse> handleForbiddenResourceAccessException(
                        ForbiddenResourceAccessException e) {
                Map<String, String> errors = Map.of("error", e.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(ErrorResponse.of("FORBIDDEN", e.getMessage(), errors));
        }

        // 기타 모든 예외를 처리하는 핸들러
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
                Map<String, String> errors = Map.of("error", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ErrorResponse.of("SERVER_ERROR", "서버 오류가 발생했습니다.", errors));
        }
}