package com.travelingdog.backend.controller;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.hc.client5.http.auth.AuthenticationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.travelingdog.backend.dto.LoginRequest;
import com.travelingdog.backend.dto.SignUpRequest;
import com.travelingdog.backend.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private LoginRequest decodeBasicAuth(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            throw new IllegalArgumentException("Basic 인증이 필요합니다.");
        }
        String base64Credentials = authHeader.substring("Basic ".length()).trim();
        byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
        String credentials = new String(credDecoded, StandardCharsets.UTF_8);
        final String[] values = credentials.split(":", 2);
        return new LoginRequest(values[0], values[1]);
    }

    @PostMapping("/signup")
    public ResponseEntity<Void> signUp(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody SignUpRequest signUpRequestBody) {
        // Basic 헤더에서 이메일/비밀번호 추출
        LoginRequest credentials = decodeBasicAuth(authHeader);
        // DTO에 Basic 인증 정보 적용
        SignUpRequest signUpRequest = new SignUpRequest(
                signUpRequestBody.nickname(),
                credentials.email(),
                credentials.password());

        String token = authService.signUp(signUpRequest);
        ResponseCookie cookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(3600)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestHeader("Authorization") String authHeader) {
        try {
            LoginRequest loginRequest = decodeBasicAuth(authHeader);
            String token = authService.login(loginRequest);

            ResponseCookie cookie = ResponseCookie.from("jwt", token)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(3600)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .build();
        } catch (AuthenticationException | BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
