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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.travelingdog.backend.dto.UserProfileDTO;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.UserRepository;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public class UserControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private String jwtCookie;

    // // Redis 설정 업데이트 - 최신 Spring Boot 형식 사용
    // @DynamicPropertySource
    // static void redisProperties(DynamicPropertyRegistry registry) {
    // // 테스트 설정 파일의 속성을 덮어쓰지 않음
    // // application-test.yml에 이미 설정된 값을 사용
    // System.out.println("사용자 컨트롤러 테스트에서 application-test.yml의 Redis 설정 사용");
    // }

    private String encodeBasic(String email, String password) {
        String credentials = email + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setup() {
        userRepository.deleteAll();

        // 테스트 사용자 생성
        testUser = User.builder()
                .nickname("testUser")
                .email("test@example.com")
                .password(passwordEncoder.encode("password123!"))
                .build();
        userRepository.save(testUser);

        // 로그인하여 JWT 토큰 획득
        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.set("Authorization", encodeBasic("test@example.com", "password123!"));

        ResponseEntity<Void> loginResponse = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginHeaders),
                Void.class);

        jwtCookie = loginResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
    }

    @Test
    void getProfile_AuthenticatedUser_ReturnsProfile() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, jwtCookie);

        // When
        ResponseEntity<UserProfileDTO> response = restTemplate.exchange(
                "/api/user/profile",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserProfileDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserProfileDTO profile = response.getBody();
        assertThat(profile).isNotNull();
        assertThat(profile.getEmail()).isEqualTo("test@example.com");
        assertThat(profile.getNickname()).isEqualTo("testUser");
    }

    @Test
    void getProfile_UnauthenticatedUser_ReturnsUnauthorized() {
        // When
        ResponseEntity<Void> response = restTemplate.getForEntity(
                "/api/user/profile",
                Void.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}