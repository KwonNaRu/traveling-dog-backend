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

/**
 * JWT 토큰 제공자 테스트
 * 
 * 이 테스트는 JWT 토큰의 생성, 검증 및 관련 기능을 검증합니다.
 * 토큰 생성, 유효한 토큰 검증, 유효하지 않은 토큰 검증, 만료된 토큰 검증 등
 * JWT 인증 시스템의 핵심 기능을 테스트합니다.
 * Mockito를 사용하여 의존성을 모의(Mock)하고 단위 테스트를 수행합니다.
 */
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