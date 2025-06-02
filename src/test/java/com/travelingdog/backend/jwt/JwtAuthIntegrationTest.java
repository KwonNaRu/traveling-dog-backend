package com.travelingdog.backend.jwt;

import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelingdog.backend.config.FirebaseConfigTest;
import com.travelingdog.backend.dto.SignUpRequest;
import com.travelingdog.backend.exception.UnauthorizedException;
import com.travelingdog.backend.model.TravelPlan;
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
@Import(FirebaseConfigTest.class)
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

    @Test
    @DisplayName("인증 없이 보호된 리소스 접근 시 401 반환")
    void testUnauthorizedAccess() throws Exception {
        // 인증 컨텍스트 설정 안함
        mockMvc.perform(get("/api/travel/plan/list"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("웹 API에 Bearer 토큰 사용 시 401 반환 (인증 방식 불일치)")
    void testWrongAuthTypeForWebAPI() throws Exception {
        // 여행 계획 데이터 생성
        createSampleTravelPlan();

        // 웹 API에 Bearer 토큰 사용 시도 - 웹 API에서는 쿠키 기반 인증만 허용하므로 예외 발생 예상
        Exception exception = assertThrows(BadCredentialsException.class, () -> {
            mockMvc.perform(get("/api/travel/plan/list")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Client-Type", "WEB"));
        });

        // 예외 메시지 검증
        assertTrue(exception.getMessage().contains("웹 API는 쿠키 기반 인증만 허용됩니다"));
    }

    @Test
    @DisplayName("앱 API에 쿠키 기반 인증 사용 시 401 반환 (인증 방식 불일치)")
    void testWrongAuthTypeForAppAPI() throws Exception {
        // 앱 API에 쿠키 기반 인증 사용 시도 - 앱 API에서는 Bearer 토큰만 허용하므로 예외 발생 예상
        Exception exception = assertThrows(BadCredentialsException.class, () -> {
            mockMvc.perform(get("/api/travel/plan/list")
                    .cookie(new Cookie("jwt", accessToken))
                    .header("X-Client-Type", "APP"));
        });

        // 예외 메시지 검증
        assertTrue(exception.getMessage().contains("앱 API는 Bearer 토큰이 필요합니다"));
    }

    @Test
    @DisplayName("웹 API에 쿠키 기반 인증 사용 시 성공")
    void testCookieBasedAuthForWebAPI() throws Exception {
        // 여행 계획 데이터 생성
        createSampleTravelPlan();

        // 웹 API에 쿠키 기반 인증 사용 - 성공 예상
        mockMvc.perform(get("/api/travel/plan/list")
                .cookie(new Cookie("jwt", accessToken))
                .header("X-Client-Type", "WEB"))
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
    @DisplayName("인증 후 여행 계획 목록 조회 테스트 (쿠키 기반 인증)")
    void testAuthenticatedTravelPlanAccess() throws Exception {
        // 이 테스트에서는 인증 컨텍스트를 명시적으로 설정
        setAuthenticationContext();

        // 여행 계획 데이터 생성
        createSampleTravelPlan();

        // 웹 API에 쿠키 기반 인증 사용 - 성공 예상
        mockMvc.perform(get("/api/travel/plan/list")
                .cookie(new Cookie("jwt", accessToken))
                .header("X-Client-Type", "WEB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("앱 API에 Bearer 토큰 사용 시 성공")
    void testBearerTokenForAppAPI() throws Exception {
        // 여행 계획 데이터 생성
        createSampleTravelPlan();

        // 앱 API에 Bearer 토큰 사용 - 성공 예상
        mockMvc.perform(get("/api/travel/plan/list")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Client-Type", "APP"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("X-Client-Type 헤더 없이 웹 요청 시 쿠키 인증 성공")
    void testNoClientTypeHeader() throws Exception {
        // 여행 계획 데이터 생성
        createSampleTravelPlan();

        // X-Client-Type 헤더 없이 쿠키 기반 인증 사용 - 기본적으로 WEB으로 간주하여 성공 예상
        mockMvc.perform(get("/api/travel/plan/list")
                .cookie(new Cookie("jwt", accessToken)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("잘못된 X-Client-Type 값으로 요청 시 기본값(WEB)으로 처리")
    void testInvalidClientType() throws Exception {
        // 여행 계획 데이터 생성
        createSampleTravelPlan();

        // 잘못된 X-Client-Type 값 사용 - 기본적으로 WEB으로 간주하여 쿠키 인증 성공 예상
        mockMvc.perform(get("/api/travel/plan/list")
                .cookie(new Cookie("jwt", accessToken))
                .header("X-Client-Type", "INVALID_TYPE"))
                .andExpect(status().isOk());

        // 잘못된 X-Client-Type 값이지만 Bearer 토큰 사용 시 실패 예상
        Exception exception = assertThrows(BadCredentialsException.class, () -> {
            mockMvc.perform(get("/api/travel/plan/list")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Client-Type", "INVALID_TYPE"));
        });

        // 예외 메시지 검증
        assertTrue(exception.getMessage().contains("웹 API는 쿠키 기반 인증만 허용됩니다"));
    }

    @Test
    @DisplayName("/api/auth/ 경로는 JWT 필터를 거치지 않음")
    void testAuthPathBypassesJwtFilter() throws Exception {
        // 인증 토큰 없이 /api/auth/ 경로에 접근해도 필터가 적용되지 않아야 함
        // 401 대신 404 또는 다른 상태 코드가 반환되어야 함(JWT 인증 필터가 적용되지 않았다는 의미)
        mockMvc.perform(get("/api/auth/status"))
                .andExpect(status().is(not(401))); // 401이 아닌 다른 상태 코드가 반환되어야 함
    }

    @Test
    @DisplayName("기본 인증(Basic Auth)은 /api/auth/app 경로에서 작동")
    void testBasicAuthWorksForAppAuthEndpoint() throws Exception {
        // Basic 인증 헤더 생성
        String plainCredentials = "test@example.com:password123";
        String base64Credentials = Base64.getEncoder().encodeToString(
                plainCredentials.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + base64Credentials;

        // /api/auth/app/login 엔드포인트는 JWT 필터를 거치지 않고 Basic 인증 처리
        mockMvc.perform(post("/api/auth/app/login")
                .header("Authorization", authHeader))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Bearer 토큰은 /api/auth/app/refresh 경로에서 작동")
    void testBearerTokenWorksForAppRefreshEndpoint() throws Exception {
        // /api/auth/app/refresh 엔드포인트는 JWT 필터를 거치지 않고 Bearer 토큰 처리
        mockMvc.perform(post("/api/auth/app/refresh")
                .header("Authorization", "Bearer " + refreshToken)
                .header("X-Client-Type", "APP"))
                .andExpect(status().isOk());
    }

    /**
     * 테스트용 샘플 여행 계획 생성
     */
    private void createSampleTravelPlan() {
        // TravelPlan 생성 예시
        TravelPlan travelPlan = TravelPlan.builder()
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