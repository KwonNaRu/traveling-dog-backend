package com.travelingdog.backend.config;

import java.io.IOException;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

import redis.embedded.RedisServer;
import redis.embedded.RedisServerBuilder;

@Configuration
@ActiveProfiles("test")
public class EmbeddedRedisConfig implements BeforeAllCallback, AfterAllCallback {

    private static RedisServer redisServer;
    private static final int REDIS_PORT = 6379;
    private static boolean started = false;
    private static final boolean IS_CI = System.getenv("CI") != null
            || Boolean.parseBoolean(System.getProperty("CI", "false"));

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (!started) {
            startRedis();
            started = true;
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        // CI 환경에서는 Redis 서버 종료 안함
        if (!IS_CI) {
            stopRedis();
        }
    }

    public static void startRedis() throws IOException {
        // CI 환경에서는 Redis 시작 안함
        if (IS_CI) {
            return;
        }

        // 이미 실행 중인지 확인
        if (redisServer != null && redisServer.isActive()) {
            return;
        }

        // OS 확인
        String osName = System.getProperty("os.name");
        boolean isWindows = osName.toLowerCase().contains("win");

        try {
            System.out.println("현재 운영체제: " + osName);
            System.out.println("사용할 Redis 포트: " + REDIS_PORT);
            System.setProperty("spring.redis.port", String.valueOf(REDIS_PORT));

            RedisServerBuilder builder = RedisServer.builder();
            builder.port(REDIS_PORT);

            // OS에 따라 다른 설정 적용
            if (isWindows) {
                // Windows용 설정
                builder.setting("maxheap 128mb");
            } else {
                // Linux용 설정 (GitHub Actions)
                builder.setting("bind 127.0.0.1");
                builder.setting("maxheap 128mb");
            }

            redisServer = builder.build();
            redisServer.start();

            System.out.println("임베디드 Redis 서버가 시작되었습니다. 포트: " + REDIS_PORT);
        } catch (Exception e) {
            System.err.println("Redis 서버 시작 실패: " + e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("bind: No such file or directory")) {
                System.err.println("Windows에서 바인딩 오류가 발생했습니다. 포트 바인딩 문제 해결 중...");

                // 다른 방식으로 시도 (일부 Windows 시스템에서 작동할 수 있음)
                try {
                    RedisServerBuilder alternativeBuilder = RedisServer.builder()
                            .port(REDIS_PORT + 1) // 다른 포트 시도
                            .setting("maxheap 128mb");

                    redisServer = alternativeBuilder.build();
                    redisServer.start();

                    System.out.println("대체 포트로 Redis 서버가 시작되었습니다. 포트: " + (REDIS_PORT + 1));
                    System.setProperty("spring.redis.port", String.valueOf(REDIS_PORT + 1));

                    return;
                } catch (Exception ex) {
                    System.err.println("대체 방식으로도 실패: " + ex.getMessage());
                }
            }

            e.printStackTrace();
            throw e;
        }
    }

    public static void stopRedis() {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
            redisServer = null;
            started = false;
            System.out.println("임베디드 Redis 서버가 중지되었습니다.");
        }
    }
}