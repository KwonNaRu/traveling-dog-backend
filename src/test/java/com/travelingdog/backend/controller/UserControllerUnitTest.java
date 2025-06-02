package com.travelingdog.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import com.travelingdog.backend.config.SecurityConfig;
import com.travelingdog.backend.dto.UserProfileDTO;
import com.travelingdog.backend.jwt.JwtAuthenticationEntryPoint;
import com.travelingdog.backend.jwt.JwtProperties;
import com.travelingdog.backend.jwt.JwtTokenProvider;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.UserRepository;
import com.travelingdog.backend.service.UserService;

@Tag("unit")
@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfig.class, UserControllerUnitTest.MockConfig.class,
        JwtAuthenticationEntryPoint.class })
public class UserControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private User testUser;

    @TestConfiguration
    static class MockConfig {
        @Bean
        public JwtTokenProvider jwtTokenProvider() {
            JwtProperties jwtProperties = new JwtProperties();
            jwtProperties.setSecretKey("test-secret-key-that-is-long-enough-for-testing-purposes-only");
            jwtProperties.setAccessTokenValidityInSeconds(3600);
            jwtProperties.setRefreshTokenValidityInSeconds(86400);

            UserRepository userRepository = mock(UserRepository.class);
            return new JwtTokenProvider(jwtProperties, userRepository);
        }

        @Bean
        public UserDetailsService userDetailsService() {
            return username -> {
                User user = new User();
                user.setId(1L);
                user.setEmail(username);
                user.setNickname("Test User");
                return user;
            };
        }
    }

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setNickname("Test User");
    }

    @Test
    @DisplayName("인증된 사용자는 프로필 정보를 조회할 수 있다")
    void getProfile_AuthenticatedUser_ReturnsProfile() throws Exception {
        // Given
        UserProfileDTO profileDTO = new UserProfileDTO();
        profileDTO.setId(1L);
        profileDTO.setEmail("test@example.com");
        profileDTO.setNickname("Test User");

        when(userService.getUserProfile(any(User.class))).thenReturn(profileDTO);

        // When & Then
        mockMvc.perform(get("/api/user/profile")
                .with(SecurityMockMvcRequestPostProcessors.user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.nickname").value("Test User"));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 프로필 정보를 조회할 수 없다")
    void getProfile_UnauthenticatedUser_ReturnsUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isUnauthorized());
    }
}