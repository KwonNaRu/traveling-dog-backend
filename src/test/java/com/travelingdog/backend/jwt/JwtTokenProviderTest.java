package com.travelingdog.backend.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.travelingdog.backend.exception.ExpiredJwtException;
import com.travelingdog.backend.exception.InvalidJwtException;
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
        // 실제 JWT 형식이지만 서명이 다른 토큰
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                "eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiZXhwIjo0ODQ1MzM5MDk4fQ." +
                "87VHrBFIESKRpR_gRxJ9IYHyW1e6exoYA0GG_UJd21I"; // 다른 시크릿 키로 서명됨

        // When
        assertThrows(InvalidJwtException.class, () -> {
            jwtTokenProvider.validateToken(invalidToken);
        });
    }

    @Test
    void expired_token_validation() throws Exception {
        // Given
        // 1. 실제 secretKey를 사용하되 만료 시간을 과거로 설정한 토큰 생성
        // 2. JwtTokenProvider를 통해 만료된 토큰 생성을 위한 메소드 추가
        String expiredToken = generateExpiredToken("test@example.com");

        // 3. 토큰이 유효한 형식인지 확인
        assertTrue(expiredToken.split("\\.").length == 3, "토큰은 header.payload.signature 형식이어야 합니다");

        // When & Then
        assertThrows(ExpiredJwtException.class, () -> {
            jwtTokenProvider.validateToken(expiredToken);
        });
    }

    /**
     * 테스트 목적으로 만료된 토큰을 생성하는 헬퍼 메소드
     */
    private String generateExpiredToken(String email) throws Exception {
        // JWT 클레임 생성 - 만료 시간을 현재 시간보다 이전으로 설정
        Date now = new java.util.Date();
        Date expired = new java.util.Date(now.getTime() - 1000 * 60 * 60); // 1시간 전 만료

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(email)
                .issueTime(now)
                .expirationTime(expired)
                .build();

        // 서명 알고리즘 및 서명 키 설정
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claimsSet);

        signedJWT.sign(new MACSigner(jwtProperties.getSecretKey().getBytes()));

        // JWT를 직렬화하여 토큰 생성
        return signedJWT.serialize();
    }

    @Test
    void malformed_token_validation() {
        // Given
        // JWT 형식이 아닌 잘못된 토큰
        String malformedToken = "not_a_valid_jwt_token";

        // When & Then
        assertThrows(InvalidJwtException.class, () -> {
            jwtTokenProvider.validateToken(malformedToken);
        });
    }
}