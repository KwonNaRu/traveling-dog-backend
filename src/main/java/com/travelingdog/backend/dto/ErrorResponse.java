package com.travelingdog.backend.dto;

import java.util.Collections;
import java.util.Map;

public record ErrorResponse(
        String code,
        String message,
        Map<String, String> errors) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, Collections.emptyMap());
    }

    public static ErrorResponse of(String code, String message, Map<String, String> errors) {
        return new ErrorResponse(code, message, errors);
    }
}