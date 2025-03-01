package com.travelingdog.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class HttpsEnforcementConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testHttpsRedirect() throws Exception {
        // HTTP 요청이 HTTPS로 리다이렉트되는지 테스트
        mockMvc.perform(MockMvcRequestBuilders.get("/api/test")
                .header("X-Forwarded-Proto", "http"))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrl("https://localhost/api/test"));
    }

    @Test
    public void testHttpsRequestIsProcessed() throws Exception {
        // HTTPS 요청은 정상적으로 처리되는지 테스트
        mockMvc.perform(MockMvcRequestBuilders.get("/api/test")
                .header("X-Forwarded-Proto", "https"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}