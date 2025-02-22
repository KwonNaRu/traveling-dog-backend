package com.travelingdog.backend.handler;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.travelingdog.backend.dto.ErrorResponse;
import com.travelingdog.backend.exception.DuplicateEmailException;

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

        @ExceptionHandler(DuplicateEmailException.class)
        public ResponseEntity<ErrorResponse> handleDuplicateEmailException(DuplicateEmailException e) {
                Map<String, String> errors = Map.of("email", e.getMessage());
                return ResponseEntity.badRequest()
                                .body(ErrorResponse.of("DUPLICATE_EMAIL", "이메일 중복 오류", errors));
        }
}