package com.travelingdog.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.redis.host=localhost",
        "spring.redis.port=6379",
        "spring.session.store-type=redis"
})
public class RedisSessionConfigTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void redisConnectionTest() {
        // Redis 연결 테스트
        Boolean result = redisTemplate.hasKey("test-key");

        // 결과가 null이 아니면 연결 성공
        assertThat(result).isNotNull();
    }

    @Test
    void redisOperationsTest() {
        // 테스트 키-값 저장
        String testKey = "test:session:key";
        String testValue = "test-value";

        // 값 저장
        redisTemplate.opsForValue().set(testKey, testValue);

        // 값 조회
        Object retrievedValue = redisTemplate.opsForValue().get(testKey);

        // 값 검증
        assertThat(retrievedValue).isEqualTo(testValue);

        // 값 삭제
        redisTemplate.delete(testKey);

        // 삭제 확인
        Boolean exists = redisTemplate.hasKey(testKey);
        assertThat(exists).isFalse();
    }
}