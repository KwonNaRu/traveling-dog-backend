package com.travelingdog.backend.controller;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

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

import com.travelingdog.backend.dto.JwtResponse;
import com.travelingdog.backend.dto.LoginRequest;
import com.travelingdog.backend.dto.SignUpRequest;
import com.travelingdog.backend.exception.InvalidRequestException;
import com.travelingdog.backend.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "회원가입 및 로그인 API")
public class AuthController {

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

        @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "회원가입 성공", content = @Content(schema = @Schema(implementation = Void.class))),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                        @ApiResponse(responseCode = "409", description = "이메일 중복")
        })
        @PostMapping("/signup")
        public ResponseEntity<Void> signUp(
                        @Parameter(description = "회원가입 정보", required = true) @Valid @RequestBody SignUpRequest signUpRequestBody) {
                // DTO에 Basic 인증 정보 적용
                SignUpRequest signUpRequest = new SignUpRequest(
                                signUpRequestBody.nickname(),
                                signUpRequestBody.email(),
                                signUpRequestBody.password());

                JwtResponse jwtResponse = authService.signUp(signUpRequest);
                ResponseCookie cookie = ResponseCookie.from("jwt", jwtResponse.accessToken())
                                .httpOnly(true)
                                .secure(true)
                                .path("/")
                                .maxAge(jwtResponse.expiresIn())
                                .build();

                return ResponseEntity.status(HttpStatus.CREATED)
                                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                                .build();
        }

        @Operation(summary = "로그인", description = "사용자 인증을 수행합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(schema = @Schema(implementation = Void.class))),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                        @ApiResponse(responseCode = "401", description = "인증 실패")
        })
        @PostMapping("/login")
        public ResponseEntity<Void> login(
                        @Parameter(description = "Basic 인증 헤더 (Base64 인코딩된 이메일:비밀번호)", required = true) @RequestHeader("Authorization") String authHeader) {
                LoginRequest loginRequest = decodeBasicAuth(authHeader);
                JwtResponse token = authService.login(loginRequest);

                ResponseCookie cookie = ResponseCookie.from("jwt", token.accessToken())
                                .httpOnly(true)
                                .secure(true)
                                .path("/")
                                .maxAge(token.expiresIn())
                                .build();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                                .build();
        }
}
