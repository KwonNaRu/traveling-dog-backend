package com.travelingdog.backend.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.travelingdog.backend.dto.JwtResponse;
import com.travelingdog.backend.dto.LoginRequest;
import com.travelingdog.backend.dto.SignUpRequest;
import com.travelingdog.backend.exception.DuplicateEmailException;
import com.travelingdog.backend.exception.RefreshTokenException;
import com.travelingdog.backend.exception.ResourceNotFoundException;
import com.travelingdog.backend.jwt.JwtProperties;
import com.travelingdog.backend.jwt.JwtTokenProvider;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public JwtResponse signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }

        User user = User.builder()
                .nickname(request.nickname())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .build();

        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        // 액세스 토큰의 실제 남은 유효 시간 계산
        long tokenRemainingTime = jwtTokenProvider.getTokenRemainingTimeInSeconds(token);

        return JwtResponse.of(token, tokenRemainingTime, refreshToken);
    }

    public JwtResponse login(LoginRequest request) throws BadCredentialsException {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtTokenProvider.generateToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        // 액세스 토큰의 실제 남은 유효 시간 계산
        long tokenRemainingTime = jwtTokenProvider.getTokenRemainingTimeInSeconds(token);

        return JwtResponse.of(token, tokenRemainingTime, refreshToken);
    }

    /**
     * 리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급합니다.
     * 
     * @param refreshToken 리프레시 토큰
     * @return 새로운 액세스 토큰 정보
     * @throws RefreshTokenException 유효하지 않은 리프레시 토큰인 경우
     */
    public JwtResponse refreshToken(String refreshToken) {
        // 리프레시 토큰 검증
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new RefreshTokenException("유효하지 않은 리프레시 토큰입니다.");
        }

        // 이메일 추출
        String email = jwtTokenProvider.extractEmail(refreshToken);

        // 새 액세스 토큰 생성
        String newAccessToken = jwtTokenProvider.generateToken(email);

        // 액세스 토큰의 실제 남은 유효 시간 계산
        long tokenRemainingTime = jwtTokenProvider.getTokenRemainingTimeInSeconds(newAccessToken);

        // 리프레시 토큰은 그대로 유지 (만료되지 않았을 경우)
        return JwtResponse.of(newAccessToken, tokenRemainingTime, refreshToken);
    }

    /**
     * 리프레시 토큰의 유효 기간을 초 단위로 반환합니다.
     * 
     * @return 리프레시 토큰 유효 기간(초)
     */
    public long getRefreshTokenValidity() {
        return jwtProperties.getRefreshTokenValidityInSeconds();
    }

    /**
     * 이메일로 사용자를 조회합니다.
     * 
     * @param email 사용자 이메일
     * @return 사용자 객체
     * @throws ResourceNotFoundException 사용자를 찾을 수 없는 경우
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}