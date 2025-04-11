package com.travelingdog.backend.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.travelingdog.backend.controller.ProtectedController;
import com.travelingdog.backend.jwt.JwtTokenProvider;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.UserRepository;
import com.travelingdog.backend.service.AuthService;

@WebMvcTest(controllers = ProtectedController.class)
@Import({ SecurityConfig.class, SecurityConfigTest.MockConfig.class })
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void accessProtectedResource_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "test@test.com", roles = "USER")
    void testAuthenticatedAccess() throws Exception {
        User mockUser = User.builder().email("test@test.com").password("password").build();
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isOk());
    }

    // 여러 개발 환경을 테스트하는 파라미터화된 테스트
    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost:3000",
            "https://traveling-dev.narudog.com",
            "https://traveling.narudog.com"
    })
    void testCorsAllowedForMultipleDevelopmentDomains(String origin) throws Exception {
        mockMvc.perform(options("/api/protected")
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origin))
                .andExpect(header().exists("Access-Control-Allow-Methods"))
                .andExpect(header().exists("Access-Control-Allow-Headers"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://unknown-domain.com",
            "http://attacker.com",
            "http://fake-frontend.com"
    })
    void testMultipleDisallowedOrigins(String disallowedOrigin) throws Exception {
        mockMvc.perform(options("/api/protected")
                .header("Origin", disallowedOrigin)
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        public AuthService authService() {
            return Mockito.mock(AuthService.class);
        }

        @Bean
        public JwtTokenProvider jwtTokenProvider() {
            return Mockito.mock(JwtTokenProvider.class);
        }

        @Bean
        public UserDetailsService userDetailsService() {
            return Mockito.mock(UserDetailsService.class);
        }

        @Bean
        public UserRepository userRepository() {
            return Mockito.mock(UserRepository.class);
        }
    }
}