package com.travelingdog.backend.controller;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * 루트 컨트롤러 테스트
 * 
 * 이 테스트는 애플리케이션의 루트 경로(/)에 대한 요청 처리를 검증합니다.
 * 루트 경로로 접근 시 프론트엔드 애플리케이션으로 리다이렉션되는지 확인합니다.
 * 백엔드와 프론트엔드 간의 기본 연결 구조가 올바르게 설정되었는지 테스트합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RootControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 루트 경로 프론트엔드 리다이렉션 테스트
     * 
     * 루트 경로(/)로 요청 시 프론트엔드 URL로 리다이렉션되는지 확인합니다.
     * HTTP 상태 코드가 3xx(리다이렉션)이고, 리다이렉션 URL이 올바른지 검증합니다.
     */
    @Test
    public void testRedirectToFrontend() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/"))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrl("http://travelingdog.duckdns.org:3000"));
    }
}
