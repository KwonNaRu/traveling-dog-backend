package com.travelingdog.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.travelingdog.backend.dto.ErrorResponse;
import com.travelingdog.backend.dto.JwtResponse;
import com.travelingdog.backend.dto.LoginRequest;
import com.travelingdog.backend.dto.SignUpRequest;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public class AuthControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 테스트 데이터 초기화
    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        userRepository.save(
                User.builder()
                        .nickname("existingUser")
                        .email("existing@test.com")
                        .password(passwordEncoder.encode("password123!"))
                        .build());
    }

    // 회원가입 성공 테스트
    @Test
    void signUp_ValidRequest_ReturnsCreated() {
        // Given
        SignUpRequest request = new SignUpRequest("newUser", "new@test.com", "password123!");

        // When
        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                Void.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(userRepository.findByEmail("new@test.com")).isPresent();
    }

    @Test
    void signUp_InvalidEmail_ReturnsBadRequest() {
        // Given
        SignUpRequest invalidRequest = new SignUpRequest("invalid-username", "invalid-email", "short");

        // When
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/auth/signup",
                invalidRequest,
                ErrorResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse.errors())
                .containsKeys("email", "password");
    }

    // 보호된 리소스 접근 테스트
    @Test
    void accessProtectedResource_WithValidToken_ReturnsOk() {
        // Given
        String token = obtainAccessToken("existing@test.com", "password123!");

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/protected",
                HttpMethod.GET,
                new HttpEntity<>(createHeaders(token)),
                String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void accessProtectedResource_WithExpiredToken_ReturnsForbidden() {
        String expiredToken = "expired.jwt.token";

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/protected",
                HttpMethod.GET,
                new HttpEntity<>(createHeaders(expiredToken)),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // 헬퍼 메서드: JWT 토큰 획득
    private String obtainAccessToken(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);
        ResponseEntity<JwtResponse> response = restTemplate.postForEntity(
                "/api/auth/login",
                request,
                JwtResponse.class);
        return response.getBody().accessToken();
    }

    // 헬퍼 메서드: 인증 헤더 생성
    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}