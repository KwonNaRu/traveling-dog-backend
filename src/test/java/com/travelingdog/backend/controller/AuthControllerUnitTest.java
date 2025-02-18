package com.travelingdog.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import com.nimbusds.jose.util.Base64;
import com.travelingdog.backend.dto.LoginRequest;
import com.travelingdog.backend.jwt.JwtTokenProvider;
import com.travelingdog.backend.service.AuthService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WebMvcTest(controllers = AuthController.class)
public class AuthControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

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
}
