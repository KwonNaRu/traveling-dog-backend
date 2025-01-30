package com.travelingdog.backend.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.travelingdog.backend.controller.ProtectedController;
import com.travelingdog.backend.jwt.JwtTokenProvider;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.UserRepository;
import com.travelingdog.backend.service.AuthService;

@WebMvcTest(controllers = ProtectedController.class, excludeAutoConfiguration = JpaAuditingConfig.class)
@Import(SecurityConfig.class)
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private UserRepository userRepository; // JPA 리포지토리 모킹

    @Test
    void accessProtectedResource_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@test.com", roles = "USER")
    void testAuthenticatedAccess() throws Exception {
        User mockUser = User.builder().email("test@test.com").password("password").build();
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isOk());
    }
}