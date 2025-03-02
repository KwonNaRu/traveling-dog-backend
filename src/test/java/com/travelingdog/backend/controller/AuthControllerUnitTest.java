package com.travelingdog.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.util.Base64;
import com.travelingdog.backend.config.SecurityConfig;
import com.travelingdog.backend.dto.JwtResponse;
import com.travelingdog.backend.dto.LoginRequest;
import com.travelingdog.backend.dto.SignUpRequest;
import com.travelingdog.backend.exception.DuplicateEmailException;
import com.travelingdog.backend.handler.GlobalExceptionHandler;
import com.travelingdog.backend.jwt.JwtTokenProvider;
import com.travelingdog.backend.service.AuthService;

/**
 * AuthController 단위 테스트
 * 
 * 이 테스트 클래스는 AuthController의 다양한 인증 관련 API 엔드포인트를 단위 테스트합니다.
 * 주요 테스트 대상:
 * 1. 로그인 API (성공 및 실패 시나리오)
 * 2. 회원가입 API (성공 및 실패 시나리오)
 * 3. 쿠키 설정 및 속성 검증
 * 
 * 이 테스트는 AuthService를 모킹하여 컨트롤러 계층만 독립적으로 테스트합니다.
 */
@Tag("unit")
@AutoConfigureMockMvc
@WebMvcTest(controllers = AuthController.class)
@Import({ SecurityConfig.class, AuthControllerUnitTest.MockConfig.class })
public class AuthControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @TestConfiguration
    static class MockConfig {
        @Bean
        public AuthService authService() {
            return mock(AuthService.class);
        }

        @Bean
        public JwtTokenProvider jwtTokenProvider() {
            return mock(JwtTokenProvider.class);
        }

        @Bean
        public UserDetailsService userDetailsService() {
            return mock(UserDetailsService.class);
        }
    }

    private String encodeBasic(String email, String password) {
        String credentials = email + ":" + password;
        return "Basic " + Base64.encode(credentials.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 각 테스트 실행 전 환경 설정
     * 
     * 1. MockMvc 설정 (GlobalExceptionHandler 포함)
     * 2. ObjectMapper 초기화
     * 
     * 이 설정을 통해 HTTP 요청 및 응답을 시뮬레이션하고 JSON 직렬화/역직렬화를 수행할 수 있습니다.
     */
    @BeforeEach
    void setUp() {
        authController = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    /**
     * 로그인 성공 시 쿠키 설정 테스트
     * 
     * 이 테스트는 유효한 로그인 정보로 로그인 API를 호출했을 때
     * 응답에 JWT 토큰이 포함된 쿠키가 설정되는지 검증합니다.
     * 
     * 테스트 과정:
     * 1. 모의 객체 설정 (로그인 성공 시 토큰 반환)
     * 2. 로그인 API 호출
     * 3. 결과 검증: 상태 코드 및 쿠키 존재 여부
     */
    @Test
    @DisplayName("로그인 성공 시 쿠키를 설정한다")
    void loginSuccess_SetCookie() throws Exception {
        String email = "test@test.com";
        String password = "password";
        String token = "dummy-token";
        String refreshToken = "dummy-refresh-token";

        when(authService.login(any(LoginRequest.class))).thenReturn(JwtResponse.of(token, 3600, refreshToken));

        mockMvc.perform(post("/api/auth/login")
                .header(HttpHeaders.AUTHORIZATION, encodeBasic(email, password)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("jwt"));
    }

    /**
     * 로그인 성공 시 쿠키 속성 검증 테스트
     * 
     * 이 테스트는 로그인 성공 시 설정되는 쿠키가
     * 올바른 속성(HttpOnly, Secure, Path)을 가지고 있는지 검증합니다.
     * 
     * 테스트 과정:
     * 1. 모의 객체 설정 (로그인 성공 시 토큰 반환)
     * 2. 로그인 API 호출
     * 3. 결과 검증: 쿠키의 HttpOnly, Secure, Path 속성
     */
    @Test
    @DisplayName("로그인 성공 시 설정된 쿠키는 올바른 속성을 가진다")
    void loginSuccess_SetCookie_WithCorrectAttributes() throws Exception {
        String email = "test@test.com";
        String password = "password";
        String token = "dummy-token";
        String refreshToken = "dummy-refresh-token";

        when(authService.login(any(LoginRequest.class))).thenReturn(JwtResponse.of(token, 3600, refreshToken));

        mockMvc.perform(post("/api/auth/login")
                .header(HttpHeaders.AUTHORIZATION, encodeBasic(email, password)))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly("jwt", true))
                .andExpect(cookie().secure("jwt", true))
                .andExpect(cookie().path("jwt", "/"));
    }

    /**
     * 로그인 실패 테스트 (잘못된 인증 정보)
     * 
     * 이 테스트는 잘못된 인증 정보로 로그인 API를 호출했을 때
     * 401 Unauthorized 응답이 반환되는지 검증합니다.
     * 
     * 테스트 과정:
     * 1. 모의 객체 설정 (로그인 실패 시 예외 발생)
     * 2. 로그인 API 호출
     * 3. 결과 검증: 401 상태 코드
     */
    @Test
    @DisplayName("잘못된 인증 정보로 로그인 시 401 응답을 반환한다")
    void loginFail_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        // Given
        String email = "wrong@test.com";
        String password = "wrongpassword";

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .header(HttpHeaders.AUTHORIZATION, encodeBasic(email, password)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 회원가입 성공 테스트
     * 
     * 이 테스트는 유효한 회원가입 정보로 회원가입 API를 호출했을 때
     * 201 Created 응답과 JWT 토큰이 포함된 쿠키가 설정되는지 검증합니다.
     * 
     * 테스트 과정:
     * 1. 모의 객체 설정 (회원가입 성공 시 토큰 반환)
     * 2. 회원가입 API 호출
     * 3. 결과 검증: 201 상태 코드 및 쿠키 존재 여부
     */
    @Test
    @DisplayName("유효한 회원가입 정보로 회원가입 시 201 응답과 쿠키를 반환한다")
    void signUp_ValidRequest_ReturnsCreated() throws Exception {
        // Given
        String email = "new@test.com";
        String password = "password123!";
        SignUpRequest request = new SignUpRequest("newUser", email, password);
        String token = "dummy-token";
        String refreshToken = "dummy-refresh-token";

        when(authService.signUp(any(SignUpRequest.class))).thenReturn(JwtResponse.of(token, 3600, refreshToken));

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                .header(HttpHeaders.AUTHORIZATION, encodeBasic(email, password))
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(cookie().exists("jwt"));
    }

    /**
     * 회원가입 실패 테스트 (중복 이메일)
     * 
     * 이 테스트는 이미 등록된 이메일로 회원가입 API를 호출했을 때
     * 400 Bad Request 응답이 반환되는지 검증합니다.
     * 
     * 테스트 과정:
     * 1. 모의 객체 설정 (중복 이메일 시 예외 발생)
     * 2. 회원가입 API 호출
     * 3. 결과 검증: 400 상태 코드 및 오류 메시지
     */
    @Test
    @DisplayName("이미 등록된 이메일로 회원가입 시 400 응답을 반환한다")
    void signUp_DuplicateEmail_ReturnsBadRequest() throws Exception {
        // Given
        String email = "existing@test.com";
        String password = "password123!";
        SignUpRequest request = new SignUpRequest("existingUser", email, password);

        when(authService.signUp(any(SignUpRequest.class)))
                .thenThrow(new DuplicateEmailException());

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                .header(HttpHeaders.AUTHORIZATION, encodeBasic(email, password))
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이메일 중복 오류"));
    }

    /**
     * Basic Auth 없는 로그인 실패 테스트
     * 
     * 이 테스트는 Authorization 헤더 없이 로그인 API를 호출했을 때
     * 400 Bad Request 응답이 반환되는지 검증합니다.
     * 
     * 테스트 과정:
     * 1. Authorization 헤더 없이 로그인 API 호출
     * 2. 결과 검증: 400 상태 코드
     */
    @Test
    @DisplayName("Basic Auth 없이 로그인 시 400 응답을 반환한다")
    void login_WithoutBasicAuth_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login"))
                .andExpect(status().isBadRequest());
    }

    /**
     * 잘못된 Basic Auth 형식으로 로그인 실패 테스트
     * 
     * 이 테스트는 잘못된 형식의 Authorization 헤더로 로그인 API를 호출했을 때
     * 400 Bad Request 응답이 반환되는지 검증합니다.
     * 
     * 테스트 과정:
     * 1. 잘못된 형식의 Authorization 헤더로 로그인 API 호출
     * 2. 결과 검증: 400 상태 코드
     */
    @Test
    @DisplayName("잘못된 Basic Auth 형식으로 로그인 시 400 응답을 반환한다")
    void login_WithInvalidBasicAuth_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .header(HttpHeaders.AUTHORIZATION, "Invalid Format"))
                .andExpect(status().isBadRequest());
    }
}
