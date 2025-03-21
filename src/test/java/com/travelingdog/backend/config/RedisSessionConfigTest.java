package com.travelingdog.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(EmbeddedRedisConfig.class)
@TestPropertySource(properties = {
        "spring.redis.host=redis",
        "spring.session.store-type=redis"
})
public class RedisSessionConfigTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 동적으로 시스템 프로퍼티에서 포트 설정을 가져옴
    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        String redisPort = System.getProperty("spring.redis.port", "6379");
        registry.add("spring.redis.port", () -> redisPort);
        System.out.println("테스트에 사용되는 Redis 포트: " + redisPort);
    }

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