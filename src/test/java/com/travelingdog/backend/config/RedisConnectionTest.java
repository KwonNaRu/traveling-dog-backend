package com.travelingdog.backend.config;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

// @SpringBootTest
// @ActiveProfiles("test")
public class RedisConnectionTest {

    // private static final Logger logger =
    // LoggerFactory.getLogger(RedisConnectionTest.class);

    // @Autowired
    // private RedisTemplate<String, Object> redisTemplate;

    // @Autowired
    // private RedisConnectionFactory connectionFactory;

    // @Test
    // public void testRedisConnection() {
    // try {
    // // 연결 정보 로깅
    // logger.info("Redis 연결 팩토리: {}", connectionFactory.getClass().getName());

    // // 간단한 Ping 테스트
    // Boolean result = redisTemplate.getConnectionFactory().getConnection().ping()
    // != null;
    // logger.info("Redis 연결 테스트 결과: {}", result);

    // // 간단한 키-값 쓰기 테스트
    // String testKey = "test:connection";
    // redisTemplate.opsForValue().set(testKey, "연결 테스트 성공!");
    // logger.info("Redis 쓰기 테스트 성공");

    // // 값 읽기 테스트
    // Object value = redisTemplate.opsForValue().get(testKey);
    // logger.info("Redis에서 읽은 값: {}", value);

    // // 키 삭제
    // redisTemplate.delete(testKey);
    // logger.info("Redis 키 삭제 성공");

    // } catch (Exception e) {
    // logger.error("Redis 연결 테스트 실패", e);
    // logger.error("연결 오류 원인: {}", e.getMessage());
    // if (e.getCause() != null) {
    // logger.error("원인 예외: {}", e.getCause().getMessage());
    // }
    // throw e;
    // }
    // }
}