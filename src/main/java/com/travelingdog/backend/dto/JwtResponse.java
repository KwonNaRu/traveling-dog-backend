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
                "Bearer", // 고정 값
                expiresIn,
                refreshToken);
    }
}
