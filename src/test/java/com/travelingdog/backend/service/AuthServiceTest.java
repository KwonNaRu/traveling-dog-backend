package com.travelingdog.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.apache.hc.client5.http.auth.AuthenticationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.travelingdog.backend.dto.LoginRequest;
import com.travelingdog.backend.dto.SignUpRequest;
import com.travelingdog.backend.exception.DuplicateEmailException;
import com.travelingdog.backend.jwt.JwtTokenProvider;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    // 회원가입 성공 테스트
    @Test
    void signUp_ValidInput_ReturnsToken() {
        // Given
        SignUpRequest request = new SignUpRequest("newUser", "new@test.com", "password123!");
        String expectedToken = "dummy.jwt.token";

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.generateToken(request.email())).thenReturn(expectedToken);

        // When
        String result = authService.signUp(request);

        // Then
        assertThat(result).isEqualTo(expectedToken);
        verify(userRepository).save(any(User.class));
        verify(jwtTokenProvider).generateToken(request.email());
    }

    // 중복 이메일 회원가입 실패 테스트
    @Test
    void signUp_DuplicateEmail_ThrowsException() {
        // Given
        SignUpRequest request = new SignUpRequest("existUser", "exist@test.com", "password123!");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("이미 가입된 이메일입니다.");
    }

    // 로그인 성공 테스트
    @Test
    void login_ValidCredentials_ReturnsToken() throws AuthenticationException {
        LoginRequest request = new LoginRequest("exist@test.com", "password123!");

        User user = User.builder()
                .email(request.email())
                .password("encodedPassword")
                .build();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);
        when(jwtTokenProvider.generateToken(user.getEmail())).thenReturn("jwt.token.here");

        String token = authService.login(request);
        assertThat(token).isEqualTo("jwt.token.here");
    }

    @Test
    void testLoadUserByUsername() {
        // Given
        User user = User.builder()
                .nickname("testuser")
                .email("test@test.com")
                .password("encodedPass")
                .build();

        // save() 메소드가 호출될 때 저장될 User 객체를 반환하도록 설정
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            return savedUser;
        });

        User savedUser = userRepository.save(user);

        when(userDetailsService.loadUserByUsername("test@test.com"))
                .thenReturn(savedUser);

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("test@test.com");

        // Then
        assertThat(userDetails.getUsername()).isEqualTo("test@test.com");
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }
}