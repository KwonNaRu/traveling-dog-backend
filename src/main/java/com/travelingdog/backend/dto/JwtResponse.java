package com.travelingdog.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JwtResponse(
        @JsonProperty("access_token") String accessToken,

        @JsonProperty("token_type") String tokenType,

        @JsonProperty("expires_in") long expiresIn,

        @JsonProperty("refresh_token") String refreshToken) {

    // 정적 팩토리 메서드 (선택적)
    public static JwtResponse of(String accessToken, long expiresIn, String refreshToken) {
        return new JwtResponse(
                accessToken,
                "JWT", // JWT 토큰 타입으로 변경
                expiresIn,
                refreshToken);
    }

    // 토큰 타입을 지정할 수 있는 확장 메서드
    public static JwtResponse of(String accessToken, String tokenType, long expiresIn, String refreshToken) {
        return new JwtResponse(
                accessToken,
                tokenType,
                expiresIn,
                refreshToken);
    }
}
