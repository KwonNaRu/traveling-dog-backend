package com.travelingdog.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.apache.hc.client5.http.auth.AuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.travelingdog.backend.dto.JwtResponse;
import com.travelingdog.backend.dto.LoginRequest;
import com.travelingdog.backend.dto.SignUpRequest;
import com.travelingdog.backend.exception.DuplicateEmailException;
import com.travelingdog.backend.jwt.JwtProperties;
import com.travelingdog.backend.jwt.JwtTokenProvider;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.UserRepository;

/**
 * AuthService 단위 테스트
 * 
 * 이 테스트 클래스는 AuthService의 다양한 인증 관련 기능을 단위 테스트합니다.
 * 주요 테스트 대상:
 * 1. 회원가입 기능 (성공 및 중복 이메일 시나리오)
 * 2. 로그인 기능 (성공 및 실패 시나리오)
 * 3. 사용자 정보 로드 기능
 * 
 * 이 테스트는 외부 의존성을 모킹하여 AuthService의 로직만 독립적으로 테스트합니다.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthService authService;

    private SignUpRequest signUpRequest;
    private User user;
    private String token;
    private String refreshToken;
    private static final long EXPECTED_TOKEN_EXPIRY = 3600L;

    /**
     * 각 테스트 실행 전 환경 설정
     * 
     * 1. 테스트용 회원가입 DTO 생성
     * 2. 테스트용 사용자 객체 생성
     * 3. 테스트용 JWT 토큰 생성
     * 
     * 이 설정을 통해 각 테스트에서 필요한 기본 객체들을 재사용할 수 있습니다.
     */
    @BeforeEach
    void setUp() {
        // 테스트용 회원가입 DTO 생성
        signUpRequest = new SignUpRequest("testUser", "test@example.com", "password");

        // 테스트용 사용자 객체 생성
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setNickname("testUser");

        // 테스트용 JWT 토큰 생성
        token = "test.jwt.token";
        refreshToken = "test.refresh.token";
    }

    /**
     * 회원가입 성공 테스트
     * 
     * 이 테스트는 유효한 회원가입 정보로 AuthService의 signUp 메소드를 호출했을 때
     * 사용자가 성공적으로 등록되고 JWT 토큰이 발급되는지 검증합니다.
     * 
     * 테스트 과정:
     * 1. 모의 객체 설정 (중복 이메일 없음, 비밀번호 인코딩, 토큰 생성)
     * 2. AuthService의 signUp 메소드 호출
     * 3. 결과 검증: 반환된 토큰이 예상한 값과 일치하는지 확인
     */
    @Test
    @DisplayName("유효한 회원가입 정보로 회원가입 시 토큰을 반환한다")
    void signUp_ValidInput_ReturnsToken() {
        // Given
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtTokenProvider.generateToken(anyString())).thenReturn(token);
        when(jwtTokenProvider.generateRefreshToken(anyString())).thenReturn(refreshToken);
        when(jwtTokenProvider.getTokenRemainingTimeInSeconds(any())).thenReturn(EXPECTED_TOKEN_EXPIRY);

        // When
        JwtResponse result = authService.signUp(signUpRequest);

        // Then
        assertNotNull(result);
        assertEquals(token, result.accessToken());
        assertEquals("JWT", result.tokenType());
        assertEquals(refreshToken, result.refreshToken());
        assertEquals(EXPECTED_TOKEN_EXPIRY, result.expiresIn());
    }

    /**
     * 중복 이메일 회원가입 실패 테스트
     * 
     * 이 테스트는 이미 등록된 이메일로 회원가입을 시도했을 때
     * DuplicateEmailException이 발생하는지 검증합니다.
     * 
     * 테스트 과정:
     * 1. 모의 객체 설정 (중복 이메일 존재)
     * 2. AuthService의 signUp 메소드 호출
     * 3. 결과 검증: DuplicateEmailException이 발생하는지 확인
     */
    @Test
    @DisplayName("이미 등록된 이메일로 회원가입 시 예외가 발생한다")
    void signUp_DuplicateEmail_ThrowsException() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // When & Then
        assertThrows(DuplicateEmailException.class, () -> {
            authService.signUp(signUpRequest);
        });
    }

    /**
     * 로그인 성공 테스트
     * 
     * 이 테스트는 유효한 로그인 정보로 AuthService의 login 메소드를 호출했을 때
     * JWT 토큰이 성공적으로 발급되는지 검증합니다.
     * 
     * 테스트 과정:
     * 1. 모의 객체 설정 (사용자 존재, 비밀번호 일치, 토큰 생성)
     * 2. AuthService의 login 메소드 호출
     * 3. 결과 검증: 반환된 토큰이 예상한 값과 일치하는지 확인
     */
    @Test
    @DisplayName("유효한 로그인 정보로 로그인 시 토큰을 반환한다")
    void login_ValidCredentials_ReturnsToken() throws AuthenticationException {
        // Given
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtTokenProvider.generateToken(anyString())).thenReturn(token);
        when(jwtTokenProvider.generateRefreshToken(anyString())).thenReturn(refreshToken);
        when(jwtTokenProvider.getTokenRemainingTimeInSeconds(any())).thenReturn(EXPECTED_TOKEN_EXPIRY);

        // When
        JwtResponse result = authService.login(loginRequest);

        // Then
        assertNotNull(result);
        assertEquals(token, result.accessToken());
        assertEquals("JWT", result.tokenType());
        assertEquals(refreshToken, result.refreshToken());
        assertEquals(EXPECTED_TOKEN_EXPIRY, result.expiresIn());
    }

    /**
     * 리프레시 토큰 갱신 테스트
     * 
     * 이 테스트는 유효한 리프레시 토큰으로 AuthService의 refreshToken 메소드를 호출했을 때
     * 새로운 액세스 토큰이 발급되는지 검증합니다.
     */
    @Test
    @DisplayName("유효한 리프레시 토큰으로 토큰 갱신 시 새 액세스 토큰을 반환한다")
    void refreshToken_ValidRefreshToken_ReturnsNewAccessToken() {
        // Given
        String validRefreshToken = "valid.refresh.token";
        String newAccessToken = "new.access.token";

        when(jwtTokenProvider.validateRefreshToken(validRefreshToken)).thenReturn(true);
        when(jwtTokenProvider.extractEmail(validRefreshToken)).thenReturn("test@example.com");
        when(jwtTokenProvider.generateToken(anyString())).thenReturn(newAccessToken);
        when(jwtTokenProvider.getTokenRemainingTimeInSeconds(newAccessToken)).thenReturn(EXPECTED_TOKEN_EXPIRY);

        // When
        JwtResponse result = authService.refreshToken(validRefreshToken);

        // Then
        assertNotNull(result);
        assertEquals(newAccessToken, result.accessToken());
        assertEquals("JWT", result.tokenType());
        assertEquals(validRefreshToken, result.refreshToken());
        assertEquals(EXPECTED_TOKEN_EXPIRY, result.expiresIn());
    }

    /**
     * 토큰 갱신 실패 테스트
     * 
     * 이 테스트는 유효하지 않은 리프레시 토큰으로 AuthService의 refreshToken 메소드를 호출했을 때
     * 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("유효하지 않은 리프레시 토큰으로 토큰 갱신 시 예외가 발생한다")
    void refreshToken_InvalidRefreshToken_ThrowsException() {
        // Given
        String invalidRefreshToken = "invalid.refresh.token";

        when(jwtTokenProvider.validateRefreshToken(invalidRefreshToken)).thenReturn(false);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            authService.refreshToken(invalidRefreshToken);
        });
    }
}