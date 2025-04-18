package com.travelingdog.backend.controller;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.travelingdog.backend.dto.JwtResponse;
import com.travelingdog.backend.dto.LoginRequest;
import com.travelingdog.backend.dto.SignUpRequest;
import com.travelingdog.backend.dto.UserProfileDTO;
import com.travelingdog.backend.exception.InvalidRequestException;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
        public ResponseEntity<UserProfileDTO> signUp(
                        @Parameter(description = "회원가입 정보", required = true) @Valid @RequestBody SignUpRequest signUpRequestBody) {
                // DTO에 Basic 인증 정보 적용
                SignUpRequest signUpRequest = new SignUpRequest(
                                signUpRequestBody.nickname(),
                                signUpRequestBody.email(),
                                signUpRequestBody.password());

                JwtResponse jwtResponse = authService.signUp(signUpRequest);

                User user = authService.getUserByEmail(signUpRequest.email());

                // 액세스 토큰 쿠키 설정
                ResponseCookie accessTokenCookie = ResponseCookie.from("jwt", jwtResponse.accessToken())
                                .httpOnly(true)
                                .secure(true)
                                .path("/")
                                .maxAge(jwtResponse.expiresIn())
                                .sameSite("None")
                                .build();

                // 리프레시 토큰 쿠키 설정
                ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", jwtResponse.refreshToken())
                                .httpOnly(true)
                                .secure(true)
                                .path("/api/auth/refresh")
                                .maxAge(authService.getRefreshTokenValidity())
                                .sameSite("None")
                                .build();

                UserProfileDTO profile = UserProfileDTO.fromEntity(user);

                return ResponseEntity.status(HttpStatus.OK)
                                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                                .body(profile);
        }

        @Operation(summary = "로그인", description = "사용자 인증을 수행합니다. Basic 인증 헤더에 Base64로 인코딩된 'email:password' 형식으로 전송하세요.", security = {
                        @SecurityRequirement(name = "basicAuth") })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(schema = @Schema(implementation = Void.class))),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                        @ApiResponse(responseCode = "401", description = "인증 실패")
        })
        @PostMapping("/login")
        public ResponseEntity<UserProfileDTO> login(
                        @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
                LoginRequest loginRequest = decodeBasicAuth(authHeader);
                JwtResponse token = authService.login(loginRequest);

                User user = authService.getUserByEmail(loginRequest.email());

                // 액세스 토큰 쿠키 설정
                ResponseCookie accessTokenCookie = ResponseCookie.from("jwt", token.accessToken())
                                .httpOnly(true)
                                .secure(true)
                                .path("/")
                                .maxAge(token.expiresIn())
                                .sameSite("None")
                                .build();

                // 리프레시 토큰 쿠키 설정
                ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", token.refreshToken())
                                .httpOnly(true)
                                .secure(true)
                                .path("/api/auth/refresh")
                                .maxAge(authService.getRefreshTokenValidity())
                                .sameSite("None")
                                .build();

                UserProfileDTO profile = UserProfileDTO.fromEntity(user);

                return ResponseEntity.status(HttpStatus.OK)
                                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                                .body(profile);
        }

        @Operation(summary = "토큰 갱신", description = "리프레시 토큰을 사용하여 액세스 토큰을 갱신합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "토큰 갱신 성공", content = @Content(schema = @Schema(implementation = Void.class))),
                        @ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰")
        })
        @PostMapping("/refresh")
        public ResponseEntity<Void> refreshToken(HttpServletRequest request) {
                // 리프레시 토큰 쿠키에서 가져오기
                String refreshToken = null;
                jakarta.servlet.http.Cookie[] cookies = request.getCookies();

                if (cookies != null) {
                        for (jakarta.servlet.http.Cookie cookie : cookies) {
                                if ("refresh_token".equals(cookie.getName())) {
                                        refreshToken = cookie.getValue();
                                        break;
                                }
                        }
                }

                if (refreshToken == null) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }

                // 리프레시 토큰 검증 및 새 액세스 토큰 발급
                JwtResponse newTokenResponse = authService.refreshToken(refreshToken);

                // 새 액세스 토큰 쿠키 설정
                ResponseCookie accessTokenCookie = ResponseCookie.from("jwt", newTokenResponse.accessToken())
                                .httpOnly(true)
                                .secure(true)
                                .path("/")
                                .maxAge(newTokenResponse.expiresIn())
                                .sameSite("None")
                                .build();

                return ResponseEntity.status(HttpStatus.OK)
                                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                                .build();
        }

        @Operation(summary = "로그아웃", description = "사용자 로그아웃을 수행합니다. 토큰을 무효화하고 JWT 쿠키를 삭제합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "로그아웃 성공", content = @Content(schema = @Schema(implementation = Void.class)))
        })
        @PostMapping("/logout")
        public ResponseEntity<Void> logout(HttpServletRequest request) {
                // JWT 쿠키 삭제
                ResponseCookie accessTokenCookie = ResponseCookie.from("jwt", "")
                                .httpOnly(true)
                                .secure(true)
                                .path("/")
                                .maxAge(0)
                                .sameSite("None")
                                .build();

                // 리프레시 토큰 쿠키 삭제
                ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", "")
                                .httpOnly(true)
                                .secure(true)
                                .path("/api/auth/refresh")
                                .maxAge(0)
                                .sameSite("None")
                                .build();

                return ResponseEntity.status(HttpStatus.OK)
                                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                                .build();
        }
}
