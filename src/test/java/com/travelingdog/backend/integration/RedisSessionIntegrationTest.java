package com.travelingdog.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.UserRepository;
import com.travelingdog.backend.service.SessionService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.redis.host=localhost",
        "spring.redis.port=6379",
        "spring.session.store-type=redis"
})
public class RedisSessionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private User testUser;
    private String authHeader;
    private static final String TEST_KEY_PREFIX = "token:*";
    private static final String USER_KEY_PREFIX = "user:*";

    private String encodeBasic(String email, String password) {
        String credentials = email + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = User.builder()
                .email("session-test@example.com")
                .password(passwordEncoder.encode("password123!"))
                .nickname("SessionTestUser")
                .build();
        userRepository.save(testUser);

        // 인증 헤더 생성
        authHeader = encodeBasic("session-test@example.com", "password123!");

        // 테스트 시작 전 Redis 데이터 정리
        cleanupRedisTestData();
    }

    @AfterEach
    void tearDown() {
        // 테스트 사용자 삭제
        userRepository.deleteAll();

        // 테스트 후 Redis 데이터 정리
        cleanupRedisTestData();
    }

    /**
     * 테스트 관련 Redis 데이터를 정리합니다.
     */
    private void cleanupRedisTestData() {
        // 테스트 관련 키 패턴으로 모든 키 삭제
        Set<String> tokenKeys = redisTemplate.keys(TEST_KEY_PREFIX);
        Set<String> userKeys = redisTemplate.keys(USER_KEY_PREFIX);

        if (tokenKeys != null && !tokenKeys.isEmpty()) {
            redisTemplate.delete(tokenKeys);
        }

        if (userKeys != null && !userKeys.isEmpty()) {
            redisTemplate.delete(userKeys);
        }
    }

    @Test
    void loginAndAccessProtectedResource_WithJwtToken_ShouldSucceed() throws Exception {
        // 1. 로그인 요청
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andReturn();

        // 2. JWT 쿠키 가져오기
        String jwtCookie = loginResult.getResponse().getHeader("Set-Cookie");
        assertThat(jwtCookie).isNotNull();

        // 쿠키에서 JWT 토큰 추출
        String jwtToken = extractJwtFromCookie(jwtCookie);

        // 3. Redis에 토큰이 저장되었는지 확인
        Long userId = sessionService.getUserIdByToken(jwtToken);
        assertThat(userId).isNotNull();

        // 4. JWT 토큰을 사용하여 보호된 리소스 접근
        mockMvc.perform(get("/api/user/profile")
                .cookie(new MockCookie("jwt", jwtToken)))
                .andExpect(status().isOk());
    }

    @Test
    void logout_ShouldInvalidateToken() throws Exception {
        // 1. 로그인 요청
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andReturn();

        // 2. JWT 쿠키 가져오기
        String jwtCookie = loginResult.getResponse().getHeader("Set-Cookie");
        String jwtToken = extractJwtFromCookie(jwtCookie);

        // 3. 로그아웃 요청
        mockMvc.perform(post("/api/auth/logout")
                .cookie(new MockCookie("jwt", jwtToken)))
                .andExpect(status().isOk());

        // 4. Redis에서 토큰이 삭제되었는지 확인
        boolean isTokenValid = sessionService.isTokenValid(jwtToken);
        assertThat(isTokenValid).isFalse();

        // 5. 토큰이 무효화된 후 보호된 리소스 접근 시도
        mockMvc.perform(get("/api/user/profile")
                .cookie(new MockCookie("jwt", jwtToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void accessProtectedResource_WithoutToken_ShouldFail() throws Exception {
        // 토큰 없이 보호된 리소스 접근 시도
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isForbidden());
    }

    // 쿠키 문자열에서 JWT 토큰 추출
    private String extractJwtFromCookie(String cookieString) {
        // "jwt=토큰값; Path=/; HttpOnly; Secure; Max-Age=3600" 형식에서 토큰값 추출
        int startIndex = cookieString.indexOf("jwt=") + 4;
        int endIndex = cookieString.indexOf(";", startIndex);
        return cookieString.substring(startIndex, endIndex);
    }
}