package com.travelingdog.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
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
import com.travelingdog.backend.dto.SignUpRequest;
import com.travelingdog.backend.dto.UserProfileDTO;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.UserRepository;

@Tag("integration")
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

    private String encodeBasic(String email, String password) {
        String credentials = email + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

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

    @Test
    void signUp_ValidRequest_ReturnsCreated() {
        // Given
        SignUpRequest request = new SignUpRequest("newUser", "new@test.com", "password123!");

        // When
        ResponseEntity<UserProfileDTO> response = restTemplate.exchange(
                "/api/auth/signup",
                HttpMethod.POST,
                new HttpEntity<>(request),
                UserProfileDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE)).isNotEmpty();
        assertThat(response.getBody()).isNotNull();
        assertThat(userRepository.findByEmail("new@test.com")).isPresent();
    }

    @Test
    void signUp_InvalidEmail_ReturnsBadRequest() {
        // Given
        SignUpRequest request = new SignUpRequest("invalid-username", "invalid-email", "short");

        // When
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/auth/signup",
                HttpMethod.POST,
                new HttpEntity<>(request),
                ErrorResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse.errors())
                .containsKeys("email", "password");
    }

    @Test
    void login_ValidCredentials_ReturnsOkWithCookie() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", encodeBasic("existing@test.com", "password123!"));

        // When
        ResponseEntity<UserProfileDTO> response = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                UserProfileDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE)).isNotEmpty();
    }

    @Test
    void accessProtectedResource_WithValidCookie_ReturnsOk() {
        // Given
        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.set("Authorization", encodeBasic("existing@test.com", "password123!"));

        ResponseEntity<Void> loginResponse = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginHeaders),
                Void.class);

        String cookie = loginResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);

        // When
        HttpHeaders protectedHeaders = new HttpHeaders();
        protectedHeaders.add(HttpHeaders.COOKIE, cookie);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/protected",
                HttpMethod.GET,
                new HttpEntity<>(protectedHeaders),
                String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void accessProtectedResource_WithoutCookie_ReturnsForbidden() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/protected", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}