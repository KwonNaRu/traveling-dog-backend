package com.travelingdog.backend.jwt;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelingdog.backend.dto.SignUpRequest;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.TravelPlanRepository;
import com.travelingdog.backend.repository.UserRepository;
import com.travelingdog.backend.service.AuthService;
import com.travelingdog.backend.status.PlanStatus;

import jakarta.servlet.http.Cookie;

/**
 * JWT 인증 통합 테스트
 * 
 * 이 테스트 클래스는 JWT 인증의 다양한 측면을 통합 테스트합니다:
 * 1. 유효한 JWT 토큰을 사용한 인증
 * 2. 쿠키 기반 인증
 * 3. 유효하지 않은 토큰 처리
 * 4. 토큰 만료 처리
 * 5. 리프레시 토큰을 사용한 액세스 토큰 갱신
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Tag("integration")
public class JwtAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private TravelPlanRepository travelPlanRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private String accessToken;
    private String refreshToken;

    @BeforeEach
    void setUp() {
        // 매 테스트 시작 전 SecurityContext 초기화
        SecurityContextHolder.clearContext();

        // 테스트용 사용자 생성 및 저장
        testUser = User.builder()
                .email("test@example.com")
                .nickname("TestUser")
                .password(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(testUser);
        userRepository.flush();

        // 실제 JWT 토큰 생성
        accessToken = jwtTokenProvider.generateToken(testUser.getEmail());
        refreshToken = jwtTokenProvider.generateRefreshToken(testUser.getEmail());

        // 인증 컨텍스트는 각 테스트에서 필요할 때만 설정
    }

    @AfterEach
    void tearDown() {
        // 테스트 종료 후 SecurityContext 정리
        userRepository.deleteAll();
        SecurityContextHolder.clearContext();
    }

    /**
     * 인증 컨텍스트를 설정하는 헬퍼 메소드
     */
    private void setAuthenticationContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities()));
    }

    /**
     * 인증이 필요한 API 호출을 위한 헬퍼 메소드
     */
    private MockHttpServletRequestBuilder authenticatedRequest(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + accessToken);
    }

    /**
     * 쿠키 기반 인증 요청 헬퍼 메소드
     */
    private MockHttpServletRequestBuilder cookieBasedRequest(MockHttpServletRequestBuilder request) {
        return request.cookie(new Cookie("jwt", accessToken));
    }

    @Test
    @DisplayName("인증 없이 보호된 리소스 접근 시 401 반환")
    void testUnauthorizedAccess() throws Exception {
        // 인증 컨텍스트 설정 안함
        mockMvc.perform(get("/api/travel/plans"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("유효한 JWT 토큰을 Authorization 헤더에 포함하여 보호된 리소스 접근 시 401 반환")
    void testAuthorizedAccess() throws Exception {
        // 여행 계획 데이터 생성 (예시)
        createSampleTravelPlan();

        mockMvc.perform(authenticatedRequest(get("/api/travel/plans")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("유효하지 않은 JWT 토큰으로 보호된 리소스 접근 시 401 반환")
    void testInvalidToken() throws Exception {
        // 인증 컨텍스트 설정 안함 (이미 초기화되어 있음)
        mockMvc.perform(get("/api/travel/plans")
                .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                        "eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiZXhwIjo0ODQ1MzM5MDk4fQ." +
                        "invalid_signature_but_valid_format"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("쿠키 기반 인증으로 보호된 리소스 접근")
    void testCookieBasedAuth() throws Exception {
        // 여행 계획 데이터 생성 (예시)
        createSampleTravelPlan();

        mockMvc.perform(cookieBasedRequest(get("/api/travel/plans")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("액세스 토큰이 유효한지 검증")
    void testTokenValidation() throws Exception {
        boolean isValid = jwtTokenProvider.validateToken(accessToken);
        assertTrue(isValid);
    }

    @Test
    @DisplayName("회원가입 API를 통한 JWT 토큰 발급")
    void testSignupTokenGeneration() throws Exception {
        // 회원가입 요청 생성
        SignUpRequest signUpRequest = new SignUpRequest(
                "newUser",
                "new@example.com",
                "password123");

        // 회원가입 API 호출
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // 쿠키 확인
        Cookie cookie = result.getResponse().getCookie("jwt");
        assertNotNull(cookie);
    }

    @Test
    @DisplayName("로그인 API를 통한 JWT 토큰 발급")
    void testLoginTokenGeneration() throws Exception {
        try {
            // Basic 인증 헤더 생성 (이메일:비밀번호를 Base64로 인코딩)
            String plainPassword = "password123";
            String authCredentials = testUser.getEmail() + ":" + plainPassword;
            String base64Credentials = Base64.getEncoder().encodeToString(
                    authCredentials.getBytes(StandardCharsets.UTF_8));
            String authHeader = "Basic " + base64Credentials;

            // 로그인 API 호출 (Authorization 헤더 사용)
            MvcResult result = mockMvc.perform(post("/api/auth/login")
                    .header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andReturn();

            // 응답 확인
            String responseBody = result.getResponse().getContentAsString();

            // 응답에서 사용자 정보 확인 (UserProfileDTO를 반환함)
            assertTrue(responseBody.contains("nickname"), "응답에 nickname 필드가 포함되어야 합니다");
            assertTrue(responseBody.contains(testUser.getNickname()), "응답에 올바른 사용자 닉네임이 포함되어야 합니다");

            // 쿠키 확인
            Cookie cookie = result.getResponse().getCookie("jwt");
            assertNotNull(cookie, "JWT 쿠키가 응답에 포함되어야 합니다");
        } catch (Exception e) {
            System.err.println("테스트 실패: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    @DisplayName("리프레시 토큰을 사용하여 새 액세스 토큰 발급")
    void testRefreshToken() throws Exception {
        try {

            // 리프레시 토큰으로 새 액세스 토큰 요청 (쿠키 사용)
            MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                    .cookie(new Cookie("refresh_token", refreshToken)))
                    .andExpect(status().isOk())
                    .andReturn();

            // 쿠키 확인
            Cookie jwtCookie = result.getResponse().getCookie("jwt");
            assertNotNull(jwtCookie, "JWT 쿠키가 응답에 포함되어야 합니다");
            assertTrue(jwtCookie.getMaxAge() > 0, "쿠키 유효 기간은 0보다 커야 합니다");
        } catch (Exception e) {
            System.err.println("테스트 실패: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰으로 요청 시 401 반환")
    void testInvalidRefreshToken() throws Exception {
        try {
            // 유효하지 않은 리프레시 토큰으로 요청
            mockMvc.perform(post("/api/auth/refresh")
                    .cookie(new Cookie("refresh_token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                            "eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiZXhwIjo0ODQ1MzM5MDk4LCJ0b2tlbl90eXBlIjoicmVmcmVzaCJ9."
                            +
                            "invalid_signature_but_valid_format")))
                    .andExpect(status().isUnauthorized());
        } catch (Exception e) {
            System.err.println("테스트 실패 (testInvalidRefreshToken): " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    @DisplayName("리프레시 토큰 쿠키가 없는 경우 401 반환")
    void testMissingRefreshToken() throws Exception {
        try {
            // 리프레시 토큰 쿠키 없이 요청
            mockMvc.perform(post("/api/auth/refresh"))
                    .andExpect(status().isUnauthorized());
        } catch (Exception e) {
            System.err.println("테스트 실패 (testMissingRefreshToken): " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    @DisplayName("인증 후 여행 계획 목록 조회 테스트")
    void testAuthenticatedTravelPlanAccess() throws Exception {
        // 이 테스트에서는 인증 컨텍스트를 명시적으로 설정
        setAuthenticationContext();

        // 여행 계획 데이터 생성 (예시)
        createSampleTravelPlan();

        mockMvc.perform(cookieBasedRequest(get("/api/travel/plans")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Authorization 헤더에 포함된 JWT 토큰으로 보호된 리소스 접근 시 401 반환")
    void testUnauthenticatedTravelPlanAccess() throws Exception {
        // 이 테스트에서는 인증 컨텍스트를 명시적으로 설정
        setAuthenticationContext();

        // 여행 계획 데이터 생성 (예시)
        createSampleTravelPlan();

        mockMvc.perform(authenticatedRequest(get("/api/travel/plans")))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 테스트용 샘플 여행 계획 생성
     */
    private void createSampleTravelPlan() {
        // TravelPlan 생성 예시
        com.travelingdog.backend.model.TravelPlan travelPlan = com.travelingdog.backend.model.TravelPlan.builder()
                .title("제주도 여행")
                .country("Korea")
                .city("제주시")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .status(PlanStatus.PRIVATE)
                .user(testUser)
                .build();

        // 저장
        travelPlanRepository.save(travelPlan);
    }
}