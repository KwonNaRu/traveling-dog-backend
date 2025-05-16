package com.travelingdog.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JwtResponse(
        @JsonProperty("access_token") String accessToken,

        @JsonProperty("expires_in") long expiresIn,

        @JsonProperty("refresh_token") String refreshToken) {

    public static JwtResponse of(String accessToken, long expiresIn, String refreshToken) {
        return new JwtResponse(
                accessToken,
                expiresIn,
                refreshToken);
    }
}
