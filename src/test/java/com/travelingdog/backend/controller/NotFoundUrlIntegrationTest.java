package com.travelingdog.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.travelingdog.backend.config.FirebaseConfigTest;
import com.travelingdog.backend.config.WithMockCustomUser;

/**
 * 존재하지 않는 URL 테스트
 * 
 * 이 테스트는 애플리케이션에서 존재하지 않는 URL로 요청이 들어왔을 때
 * 404 상태 코드와 적절한 응답 형식으로 처리하는지 검증합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
@Import(FirebaseConfigTest.class)
public class NotFoundUrlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockCustomUser
    @DisplayName("존재하지 않는 URL로 요청시 404 응답 확인")
    void testNonExistentUrlReturns404() throws Exception {
        // '/trip' 같은 존재하지 않는 URL 요청
        mockMvc.perform(get("/trip")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("요청한 리소스를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.errors.path").isNotEmpty())
                .andExpect(jsonPath("$.errors.method").value("GET"));

        // 또 다른 존재하지 않는 URL 테스트
        mockMvc.perform(get("/api/non-existent")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("요청한 리소스를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.errors.path").isNotEmpty())
                .andExpect(jsonPath("$.errors.method").value("GET"));
    }
}