package com.travelingdog.backend.service;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.travelingdog.backend.config.FirebaseConfig;
import com.travelingdog.backend.dto.JwtResponse;
import com.travelingdog.backend.dto.LoginRequest;
import com.travelingdog.backend.dto.SignUpRequest;
import com.travelingdog.backend.exception.DuplicateEmailException;
import com.travelingdog.backend.exception.InvalidRequestException;
import com.travelingdog.backend.exception.RefreshTokenException;
import com.travelingdog.backend.exception.ResourceNotFoundException;
import com.travelingdog.backend.jwt.JwtProperties;
import com.travelingdog.backend.jwt.JwtTokenProvider;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RestClient restClient;
    private final FirebaseAuth firebaseAuth;

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

        return JwtResponse.of(token, tokenRemainingTime, refreshToken, user.getEmail());
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

        return JwtResponse.of(token, tokenRemainingTime, refreshToken, user.getEmail());
    }

    /**
     * 소셜 로그인을 처리합니다.
     * 
     * @param provider 소셜 로그인 제공자 (google, kakao, naver 등)
     * @param token    소셜 로그인 제공자로부터 받은 토큰
     * @return JWT 응답
     */
    public JwtResponse processSocialLogin(String provider, String token) {
        // 소셜 로그인 제공자별 사용자 정보 추출
        Map<String, String> userInfo;

        switch (provider.toLowerCase()) {
            case "google":
                userInfo = getGoogleUserInfo(token);
                break;
            // case "kakao":
            // userInfo = getKakaoUserInfo(token);
            // break;
            case "naver":
                userInfo = getNaverUserInfo(token);
                break;
            default:
                throw new InvalidRequestException("지원하지 않는 소셜 로그인 제공자입니다: " + provider);
        }

        String email = userInfo.get("email");
        String nickname = userInfo.get("nickname");

        if (email == null || email.isBlank()) {
            throw new InvalidRequestException("소셜 로그인에서 이메일을 가져올 수 없습니다.");
        }

        // 기존 사용자인지 확인 후 처리
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            // 신규 사용자 등록
            user = User.builder()
                    .email(email)
                    .nickname(nickname != null ? nickname : email.split("@")[0])
                    .password(passwordEncoder.encode("SOCIAL_LOGIN_" + System.currentTimeMillis()))
                    .build();
            userRepository.save(user);
        }

        String jwtToken = jwtTokenProvider.generateToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        // 액세스 토큰의 실제 남은 유효 시간 계산
        long tokenRemainingTime = jwtTokenProvider.getTokenRemainingTimeInSeconds(jwtToken);

        return JwtResponse.of(jwtToken, tokenRemainingTime, refreshToken, user.getEmail());
    }

    /**
     * Google Firebase ID 토큰을 검증하고 사용자 정보를 가져옵니다.
     * FirebaseAuth가 초기화되지 않은 경우 토큰에서 직접 정보를 추출합니다.
     */
    private Map<String, String> getGoogleUserInfo(String token) {
        try {
            log.info("Firebase ID 토큰 검증 시작");

            // Firebase Auth가 초기화되지 않은 경우
            if (firebaseAuth == null) {
                log.warn("FirebaseAuth가 초기화되지 않았습니다. 토큰에서 직접 정보를 추출합니다.");
                return extractUserInfoFromIdToken(token);
            }

            // Firebase Admin SDK를 사용하여 ID 토큰 검증
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(token);

            log.info("Firebase ID 토큰 검증 성공: UID={}", decodedToken.getUid());

            // 사용자 정보 추출
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();

            // 이름이 없는 경우 displayName 사용
            if (name == null || name.isBlank()) {
                name = decodedToken.getClaims().getOrDefault("name", "").toString();
            }

            // 이름이 여전히 없는 경우 이메일 앞부분 사용
            if (name == null || name.isBlank()) {
                name = email.split("@")[0];
            }

            log.info("Firebase 사용자 정보 추출 완료: 이메일={}", email);

            return Map.of(
                    "email", email,
                    "nickname", name);

        } catch (Exception e) {
            log.error("Firebase 토큰 처리 중 오류 발생: {}", e.getMessage());
            // Firebase Admin SDK가 실패하면 직접 토큰 디코딩 시도
            log.info("토큰 직접 디코딩 시도");
            return extractUserInfoFromIdToken(token);
        }
    }

    /**
     * ID 토큰에서 직접 사용자 정보를 추출합니다.
     * Firebase Admin SDK를 사용할 수 없을 때 대체 방법으로 사용됩니다.
     */
    private Map<String, String> extractUserInfoFromIdToken(String idToken) {
        try {
            // JWT 토큰의 두 번째 부분(페이로드)을 디코딩
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new InvalidRequestException("유효하지 않은 ID 토큰 형식입니다.");
            }

            // Base64 디코딩 (패딩 문제 해결)
            String payload = parts[1];
            int paddingLength = (4 - payload.length() % 4) % 4;
            String paddedPayload = payload;
            if (paddingLength > 0) {
                paddedPayload = payload + "=".repeat(paddingLength);
            }

            // 디코딩된 페이로드를 UTF-8 문자열로 변환
            byte[] decodedBytes = java.util.Base64.getUrlDecoder().decode(paddedPayload);
            String decodedPayload = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);

            // JSON 파싱
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> payloadMap = mapper.readValue(decodedPayload, Map.class);

            log.info("ID 토큰 직접 디코딩 성공");

            // 필요한 사용자 정보 추출
            String email = (String) payloadMap.get("email");

            // 이름 정보 추출 시도
            String name = null;
            if (payloadMap.containsKey("name")) {
                name = (String) payloadMap.get("name");
            } else if (payloadMap.containsKey("given_name")) {
                name = (String) payloadMap.get("given_name");
            }

            // 이메일이 없는 경우 오류
            if (email == null || email.isBlank()) {
                throw new InvalidRequestException("ID 토큰에서 이메일을 찾을 수 없습니다.");
            }

            // 이름이 없는 경우 이메일의 @ 앞부분 사용
            if (name == null || name.isBlank()) {
                name = email.split("@")[0];
            }

            return Map.of(
                    "email", email,
                    "nickname", name);
        } catch (Exception e) {
            log.error("ID 토큰 디코딩 중 오류 발생: {}", e.getMessage());
            throw new InvalidRequestException("ID 토큰 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // /**
    // * Kakao OAuth에서 사용자 정보를 가져옵니다.
    // */
    // private Map<String, String> getKakaoUserInfo(String token) {
    // HttpHeaders headers = new HttpHeaders();
    // headers.setBearerAuth(token);
    // HttpEntity<String> entity = new HttpEntity<>(headers);

    // ResponseEntity<Map> response =
    // restClient.get().uri("https://kapi.kakao.com/v2/user/me")
    // .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
    // .retrieve()
    // .body(Map.class);

    // Map<String, Object> userAttributes = response.getBody();
    // Map<String, Object> kakaoAccount = (Map<String, Object>)
    // userAttributes.get("kakao_account");
    // Map<String, Object> profile = (Map<String, Object>)
    // kakaoAccount.get("profile");

    // return Map.of(
    // "email", (String) kakaoAccount.get("email"),
    // "nickname", (String) profile.get("nickname"));
    // }

    /**
     * Naver OAuth에서 사용자 정보를 가져옵니다.
     */
    private Map<String, String> getNaverUserInfo(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        Map<String, Object> response = restClient.get().uri("https://openapi.naver.com/v1/nid/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(Map.class);

        return Map.of(
                "email", (String) response.get("email"),
                "nickname", (String) response.get("nickname"));
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
        return JwtResponse.of(newAccessToken, tokenRemainingTime, refreshToken, email);
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