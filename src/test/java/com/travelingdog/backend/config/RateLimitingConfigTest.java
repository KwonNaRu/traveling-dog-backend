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
public class RateLimitingConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testRateLimitingAllowsRequestsWithinLimit() throws Exception {
        // 허용된 요청 횟수 내에서는 정상 처리되는지 테스트
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/test")
                    .header("X-Forwarded-For", "192.168.1.1"))
                    .andExpect(MockMvcResultMatchers.status().isOk());

            // 각 요청 사이에 약간의 지연 시간 추가 (100ms)
            Thread.sleep(100);
        }
    }

    @Test
    public void testRateLimitingBlocksRequestsOverLimit() throws Exception {
        // 허용된 요청 횟수를 초과하면 429 상태 코드를 반환하는지 테스트
        // 먼저 허용 한도까지 요청 (초당 10개 요청 제한)
        for (int i = 0; i < 15; i++) {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/test")
                    .header("X-Forwarded-For", "192.168.1.2"));

            // 지연 시간 없이 빠르게 요청 보내기
        }

        // 한도 초과 요청 시 429 상태 코드 반환 확인
        mockMvc.perform(MockMvcRequestBuilders.get("/api/test")
                .header("X-Forwarded-For", "192.168.1.2"))
                .andExpect(MockMvcResultMatchers.status().isTooManyRequests());
    }
}