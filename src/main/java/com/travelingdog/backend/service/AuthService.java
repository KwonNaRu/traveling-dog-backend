package com.travelingdog.backend.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.travelingdog.backend.dto.JwtResponse;
import com.travelingdog.backend.dto.LoginRequest;
import com.travelingdog.backend.dto.SignUpRequest;
import com.travelingdog.backend.exception.DuplicateEmailException;
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

        return JwtResponse.of(token, 3600, refreshToken);
    }

    public JwtResponse login(LoginRequest request) throws BadCredentialsException {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtTokenProvider.generateToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        return JwtResponse.of(token, 3600, refreshToken);
    }
}