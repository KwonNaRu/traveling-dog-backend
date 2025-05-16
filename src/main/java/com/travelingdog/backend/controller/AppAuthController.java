package com.travelingdog.backend.controller;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.travelingdog.backend.dto.JwtResponse;
import com.travelingdog.backend.dto.LoginRequest;
import com.travelingdog.backend.dto.SignUpRequest;
import com.travelingdog.backend.exception.InvalidRequestException;
import com.travelingdog.backend.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/app")
@RequiredArgsConstructor
public class AppAuthController {

        private final AuthService authService;

        private LoginRequest decodeBasicAuth(String authHeader) {
                if (authHeader == null || !authHeader.startsWith("Basic ")) {
                        throw new InvalidRequestException("Basic 인증이 필요합니다.");
                }
                String base64Credentials = authHeader.substring("Basic ".length()).trim();
                byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                final String[] values = credentials.split(":", 2);
                return new LoginRequest(values[0], values[1]);
        }

        @PostMapping("/signup")
        public ResponseEntity<JwtResponse> signUp(
                        @Valid @RequestBody SignUpRequest signUpRequestBody) {
                // DTO에 Basic 인증 정보 적용
                SignUpRequest signUpRequest = new SignUpRequest(
                                signUpRequestBody.nickname(),
                                signUpRequestBody.email(),
                                signUpRequestBody.password());

                JwtResponse token = authService.signUp(signUpRequest);

                return ResponseEntity.status(HttpStatus.OK)
                                .body(token);
        }

        @PostMapping("/login")
        public ResponseEntity<JwtResponse> login(
                        @RequestHeader("Authorization") String authHeader) {
                LoginRequest loginRequest = decodeBasicAuth(authHeader);
                JwtResponse token = authService.login(loginRequest);

                return ResponseEntity.status(HttpStatus.OK)
                                .body(token);
        }

        @PostMapping("/refresh")
        public ResponseEntity<JwtResponse> refreshToken(
                        @RequestHeader("Authorization") String authHeader) {
                // Bearer 토큰에서 리프레시 토큰 추출
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                        throw new InvalidRequestException("Bearer 토큰이 필요합니다.");
                }
                String refreshToken = authHeader.substring(7); // "Bearer " 이후의 토큰 추출

                // 리프레시 토큰 검증 및 새 토큰 발급
                JwtResponse newTokenResponse = authService.refreshToken(refreshToken);

                // 새로운 액세스 토큰과 리프레시 토큰을 응답 본문에 포함하여 반환
                return ResponseEntity.ok(newTokenResponse);
        }

}
