package com.travelingdog.backend.config;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

import redis.embedded.RedisServer;
import java.io.IOException;

@Configuration
@ActiveProfiles("test")
public class EmbeddedRedisConfig implements BeforeAllCallback, AfterAllCallback {

    private static RedisServer redisServer;
    private static final int REDIS_PORT = 6379;
    private static boolean started = false;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (!started) {
            startRedis();
            started = true;
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        // 애플리케이션 종료 시 Redis 서버 종료
        // 이 메서드는 마지막 테스트가 완료된 후에만 호출됩니다
        // 모든 테스트 클래스에서 공유할 수 있도록 종료 로직은 비워둡니다
    }

    public static void startRedis() throws IOException {
        // 이미 실행 중인지 확인
        if (redisServer != null && redisServer.isActive()) {
            return;
        }

        // Windows에서 실행 중인지 확인
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        try {
            if (isWindows) {
                // Windows에서는 maxheap 설정으로 메모리 제한
                redisServer = RedisServer.builder()
                        .port(REDIS_PORT)
                        .setting("maxheap 128mb") // 메모리 제한 설정
                        .build();
            } else {
                redisServer = new RedisServer(REDIS_PORT);
            }
            redisServer.start();
        } catch (Exception e) {
            System.err.println("Redis 서버 시작 실패: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public static void stopRedis() {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
            redisServer = null;
            started = false;
        }
    }
}