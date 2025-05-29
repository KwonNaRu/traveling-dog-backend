package com.travelingdog.backend.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.travelingdog.backend.config.FirebaseConfigTest;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.UserRepository;

/**
 * 토큰 갱신 기능 테스트
 * 
 * 이 테스트는 JWT 토큰의 갱신 기능을 검증합니다.
 * 리프레시 토큰을 사용하여 액세스 토큰을 갱신하는 과정과
 * 토큰 만료 처리 등 토큰 갱신 관련 기능의 정상 작동을 확인합니다.
 * 실제 애플리케이션 컨텍스트를 로드하여 통합 테스트 환경에서 수행됩니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(FirebaseConfigTest.class)
public class TokenRefreshTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserRepository userRepository;

    private User testUser;
    private String accessToken;
    private String refreshToken;

    @BeforeEach
    public void setup() {
        // 테스트 사용자 설정
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setNickname("testuser");
        testUser.setPassword("password");

        // userRepository 모킹 설정 - 명확하게 이메일로 찾을 때 사용자 반환
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // 초기 토큰 생성
        accessToken = jwtTokenProvider.generateToken(testUser.getEmail());
        refreshToken = jwtTokenProvider.generateRefreshToken(testUser.getEmail());
    }

    @Test
    public void testRefreshTokenGeneration() {
        // 리프레시 토큰이 생성되는지 테스트
        assertNotNull(refreshToken);
        assertTrue(jwtTokenProvider.validateToken(refreshToken));
    }

    @Test
    public void testAccessTokenRefresh() {
        // 리프레시 토큰으로 새 액세스 토큰을 발급받는지 테스트
        String email = jwtTokenProvider.extractEmail(refreshToken);
        assertEquals("test@example.com", email);

        // userRepository가 이메일로 사용자를 찾을 수 있는지 확인
        assertTrue(userRepository.findByEmail(email).isPresent());

        // 테스트 실패 원인: 동일한 시간에 생성된 토큰은 동일한 값을 가질 수 있음
        // 따라서 토큰 값 비교 대신 토큰이 유효한지 확인하는 방식으로 변경
        String newAccessToken = jwtTokenProvider.refreshAccessToken(refreshToken);

        assertNotNull(newAccessToken);
        // 토큰 값 비교 대신 토큰이 유효한지 확인
        assertTrue(jwtTokenProvider.validateToken(newAccessToken));
        assertEquals(testUser.getEmail(), jwtTokenProvider.extractEmail(newAccessToken));
    }

    @Test
    public void testRefreshTokenExpiry() {
        // 리프레시 토큰의 만료 시간이 액세스 토큰보다 긴지 테스트
        long accessTokenExpiry = jwtTokenProvider.getTokenExpiry(accessToken);
        long refreshTokenExpiry = jwtTokenProvider.getTokenExpiry(refreshToken);

        assertTrue(refreshTokenExpiry > accessTokenExpiry);
    }
}