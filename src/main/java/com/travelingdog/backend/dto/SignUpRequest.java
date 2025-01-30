package com.travelingdog.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank String nickname,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password) {
}
