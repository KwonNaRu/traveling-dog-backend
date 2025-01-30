package com.travelingdog.backend.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class JwtTokenProviderTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Test
    void token_generate_and_validate() {
        // Given
        userRepository.save(User.builder()
                .email("test@example.com")
                .nickname("testuser")
                .password("password123!")
                .build());

        // When
        String token = jwtTokenProvider.generateToken("test@example.com");
        boolean isValid = jwtTokenProvider.validateToken(token);
        String extractedEmail = jwtTokenProvider.extractEmail(token);

        // Then
        assertTrue(isValid);
        assertEquals("test@example.com", extractedEmail);
    }

    @Test
    void invalid_token_validation() {
        // Given
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" + "." +
                "eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiZXhwIjoxOTkxMzM5MDk4fQ" + "." +
                "invalid_signature";

        // When
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void expired_token_validation() {
        // Given
        // 2022년 이전 날짜로 만료된 토큰
        String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" + "." +
                "eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiZXhwIjoxNjQwOTk1MDAwfQ" + "." +
                "some_signature";

        // When
        boolean isValid = jwtTokenProvider.validateToken(expiredToken);

        // Then
        assertFalse(isValid);
    }
}