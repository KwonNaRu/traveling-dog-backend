package com.travelingdog.backend.jwt;

import java.text.ParseException;
import java.util.Date;

import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.travelingdog.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;

    public String generateToken(String email) {
        try {
            Date now = new Date();
            Date validity = new Date(now.getTime() + jwtProperties.getAccessTokenValidityInSeconds() * 1000);

            // JWT Claims 설정
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(email)
                    .issueTime(now)
                    .expirationTime(validity)
                    .claim("token_type", "access")
                    .build();

            // 서명 알고리즘 및 서명 키 설정
            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claimsSet);

            signedJWT.sign(new MACSigner(jwtProperties.getSecretKey().getBytes()));

            // JWT를 직렬화하여 토큰 생성
            return signedJWT.serialize();

        } catch (JOSEException e) {
            throw new RuntimeException("토큰 생성 중 오류 발생", e);
        }
    }

    public String generateRefreshToken(String email) {
        try {
            Date now = new Date();
            Date validity = new Date(now.getTime() + jwtProperties.getRefreshTokenValidityInSeconds() * 1000);

            // JWT Claims 설정 - 리프레시 토큰용
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(email)
                    .issueTime(now)
                    .expirationTime(validity)
                    .claim("token_type", "refresh")
                    .build();

            // 서명 알고리즘 및 서명 키 설정
            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claimsSet);

            signedJWT.sign(new MACSigner(jwtProperties.getSecretKey().getBytes()));

            // JWT를 직렬화하여 토큰 생성
            return signedJWT.serialize();

        } catch (JOSEException e) {
            throw new RuntimeException("리프레시 토큰 생성 중 오류 발생", e);
        }
    }

    // 액세스 토큰 갱신
    public String refreshAccessToken(String refreshToken) {
        if (!validateRefreshToken(refreshToken)) {
            throw new RuntimeException("유효하지 않은 리프레시 토큰입니다.");
        }

        String email = extractEmail(refreshToken);
        return generateToken(email);
    }

    // 리프레시 토큰 유효성 검증
    public boolean validateRefreshToken(String token) {
        try {
            // 1. JWT 토큰 파싱
            SignedJWT signedJWT = SignedJWT.parse(token);
            MACVerifier verifier = new MACVerifier(jwtProperties.getSecretKey().getBytes());

            // 2. 토큰 서명 및 만료 확인
            if (!signedJWT.verify(verifier) || isTokenExpired(token)) {
                return false;
            }

            // 3. 토큰 타입 확인
            String tokenType = signedJWT.getJWTClaimsSet().getStringClaim("token_type");
            if (!"refresh".equals(tokenType)) {
                return false;
            }

            // 4. 토큰에서 이메일 추출
            String email = extractEmail(token);

            // 5. DB에서 사용자 확인
            return userRepository.findByEmail(email)
                    .isPresent(); // 사용자가 없으면 false 반환

        } catch (Exception e) {
            return false;
        }
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            // 1. JWT 토큰 파싱
            SignedJWT signedJWT = SignedJWT.parse(token);
            MACVerifier verifier = new MACVerifier(jwtProperties.getSecretKey().getBytes());

            // 2. 토큰 서명 및 만료 확인
            if (!signedJWT.verify(verifier) || isTokenExpired(token)) {
                return false;
            }

            // 3. 토큰에서 이메일 추출
            String email = extractEmail(token);

            // 4. DB에서 사용자 확인
            return userRepository.findByEmail(email)
                    .isPresent(); // 사용자가 없으면 false 반환

        } catch (Exception e) {
            return false;
        }
    }

    // JWT의 만료 여부를 확인하는 메소드
    private boolean isTokenExpired(String token) {
        return getClaims(token).getExpirationTime().before(new Date());
    }

    // 토큰의 만료 시간을 밀리초로 반환
    public long getTokenExpiry(String token) {
        try {
            Date expirationTime = getClaims(token).getExpirationTime();
            return expirationTime.getTime();
        } catch (Exception e) {
            throw new RuntimeException("토큰 만료 시간 추출 중 오류 발생", e);
        }
    }

    public JWTClaimsSet getClaims(String token) {
        try {
            // JWT 토큰 파싱
            SignedJWT signedJWT = SignedJWT.parse(token);

            // 서명이 유효한지 검증하고, 유효하면 클레임 반환
            return signedJWT.getJWTClaimsSet();

        } catch (ParseException e) {
            throw new RuntimeException("토큰을 파싱하는 중 오류 발생", e);
        }
    }

    // JWT에서 사용자 이메일을 추출하는 메소드
    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public String extractUsername(String token) {
        try {
            return getClaims(token).getStringClaim("username");
        } catch (ParseException e) {
            throw new RuntimeException("토큰 파싱 중 오류 발생", e);
        }
    }

    public String extractRole(String token) {
        try {
            return getClaims(token).getStringClaim("role");
        } catch (ParseException e) {
            throw new RuntimeException("토큰 파싱 중 오류 발생", e);
        }
    }
}