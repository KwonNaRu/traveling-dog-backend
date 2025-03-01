package com.travelingdog.backend.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    private User testUser;

    @BeforeEach
    void setUp() {
        // JwtProperties 설정 - lenient 모드 적용
        lenient().when(jwtProperties.getSecretKey())
                .thenReturn("asdjrkljakljvkvlwjo9iausoidapjklbkahsiodhrijasdrkljklasjdlkrja");
        lenient().when(jwtProperties.getAccessTokenValidityInSeconds()).thenReturn(86400L);

        // 테스트 사용자 생성
        testUser = User.builder()
                .email("test@example.com")
                .nickname("testuser")
                .password("password123!")
                .build();
    }

    @Test
    void token_generate_and_validate() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

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