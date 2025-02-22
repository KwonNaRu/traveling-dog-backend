package com.travelingdog.backend.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.util.Base64;
import com.travelingdog.backend.config.SecurityConfig;
import com.travelingdog.backend.dto.LoginRequest;
import com.travelingdog.backend.dto.SignUpRequest;
import com.travelingdog.backend.exception.DuplicateEmailException;
import com.travelingdog.backend.jwt.JwtTokenProvider;
import com.travelingdog.backend.service.AuthService;

@Tag("unit")
@AutoConfigureMockMvc
@WebMvcTest(controllers = AuthController.class)
@Import({ SecurityConfig.class, AuthControllerUnitTest.MockConfig.class })
public class AuthControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

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
    }

    private String encodeBasic(String email, String password) {
        String credentials = email + ":" + password;
        return "Basic " + Base64.encode(credentials.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void loginSuccess_SetCookie() throws Exception {
        String email = "test@test.com";
        String password = "password";
        String token = "dummy-token";

        when(authService.login(any(LoginRequest.class))).thenReturn(token);

        mockMvc.perform(post("/api/auth/login")
                .header(HttpHeaders.AUTHORIZATION, encodeBasic(email, password)))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));
    }

    @Test
    void loginSuccess_SetCookie_WithCorrectAttributes() throws Exception {
        String email = "test@test.com";
        String password = "password";
        String token = "dummy-token";

        when(authService.login(any(LoginRequest.class))).thenReturn(token);

        mockMvc.perform(post("/api/auth/login")
                .header(HttpHeaders.AUTHORIZATION, encodeBasic(email, password)))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        containsString("Secure")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        containsString("Path=/")));
    }

    @Test
    void loginFail_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        // Given
        String email = "wrong@test.com";
        String password = "wrongpassword";

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .header(HttpHeaders.AUTHORIZATION, encodeBasic(email, password)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signUp_ValidRequest_ReturnsCreated() throws Exception {
        // Given
        String email = "new@test.com";
        String password = "password123!";
        SignUpRequest request = new SignUpRequest("newUser", email, password);
        String token = "dummy-token";

        when(authService.signUp(any(SignUpRequest.class))).thenReturn(token);

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                .header(HttpHeaders.AUTHORIZATION, encodeBasic(email, password))
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));
    }

    @Test
    void signUp_DuplicateEmail_ReturnsBadRequest() throws Exception {
        // Given
        String email = "existing@test.com";
        String password = "password123!";
        SignUpRequest request = new SignUpRequest("existingUser", email, password);

        when(authService.signUp(any(SignUpRequest.class)))
                .thenThrow(new DuplicateEmailException());

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                .header(HttpHeaders.AUTHORIZATION, encodeBasic(email, password))
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithoutBasicAuth_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithInvalidBasicAuth_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .header(HttpHeaders.AUTHORIZATION, "Invalid Basic Auth"))
                .andExpect(status().isBadRequest());
    }
}
