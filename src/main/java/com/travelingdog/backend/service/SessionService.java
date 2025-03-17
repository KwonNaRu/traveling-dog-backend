package com.travelingdog.backend.service;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.travelingdog.backend.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private static final String TOKEN_PREFIX = "token:";
    private static final String USER_PREFIX = "user:";
    private static final int TOKEN_EXPIRATION = 3600; // 1시간

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 사용자의 JWT 토큰을 Redis에 저장합니다.
     * 
     * @param user           사용자 객체
     * @param token          JWT 토큰
     * @param expirationTime 만료 시간(초)
     */
    public void saveToken(User user, String token, long expirationTime) {
        String tokenKey = TOKEN_PREFIX + user.getId();
        String userKey = USER_PREFIX + token;

        // 토큰을 사용자 ID로 조회할 수 있도록 저장
        redisTemplate.opsForValue().set(tokenKey, token);
        redisTemplate.expire(tokenKey, expirationTime, TimeUnit.SECONDS);

        // 토큰으로 사용자 ID를 조회할 수 있도록 저장
        redisTemplate.opsForValue().set(userKey, user.getId());
        redisTemplate.expire(userKey, expirationTime, TimeUnit.SECONDS);

        log.debug("Redis에 토큰 저장: 사용자 ID={}", user.getId());
    }

    /**
     * 토큰으로 사용자 ID를 조회합니다.
     * 
     * @param token JWT 토큰
     * @return 사용자 ID 또는 토큰이 없는 경우 null
     */
    public Long getUserIdByToken(String token) {
        String userKey = USER_PREFIX + token;
        Object value = redisTemplate.opsForValue().get(userKey);

        // Redis에서 가져온 값이 없는 경우
        if (value == null) {
            return null;
        }

        // Integer를 Long으로 변환
        Long userId;
        if (value instanceof Integer) {
            userId = ((Integer) value).longValue();
        } else if (value instanceof Long) {
            userId = (Long) value;
        } else {
            log.warn("예상치 못한 타입의 사용자 ID: {}", value.getClass().getName());
            return null;
        }

        log.debug("토큰으로 사용자 ID 조회: {}", userId);
        return userId;
    }

    /**
     * 사용자 ID로 토큰을 조회합니다.
     * 
     * @param userId 사용자 ID
     * @return JWT 토큰 또는 없는 경우 null
     */
    public String getTokenByUserId(Long userId) {
        String tokenKey = TOKEN_PREFIX + userId;
        String token = (String) redisTemplate.opsForValue().get(tokenKey);
        log.debug("사용자 ID로 토큰 조회: {}", userId);
        return token;
    }

    /**
     * 토큰을 무효화합니다.
     * 
     * @param token JWT 토큰
     */
    public void invalidateToken(String token) {
        String userKey = USER_PREFIX + token;
        Object value = redisTemplate.opsForValue().get(userKey);

        // Redis에서 가져온 값이 없는 경우
        if (value == null) {
            return;
        }

        // Integer를 Long으로 변환
        Long userId;
        if (value instanceof Integer) {
            userId = ((Integer) value).longValue();
        } else if (value instanceof Long) {
            userId = (Long) value;
        } else {
            log.warn("예상치 못한 타입의 사용자 ID: {}", value.getClass().getName());
            return;
        }

        if (userId != null) {
            String tokenKey = TOKEN_PREFIX + userId;
            redisTemplate.delete(tokenKey);
            redisTemplate.delete(userKey);
            log.debug("토큰 무효화: 사용자 ID={}", userId);
        }
    }

    /**
     * 사용자의 모든 토큰을 무효화합니다.
     * 
     * @param userId 사용자 ID
     */
    public void invalidateAllUserTokens(Long userId) {
        String tokenKey = TOKEN_PREFIX + userId;
        String token = (String) redisTemplate.opsForValue().get(tokenKey);

        if (token != null) {
            String userKey = USER_PREFIX + token;
            redisTemplate.delete(tokenKey);
            redisTemplate.delete(userKey);
            log.debug("사용자의 모든 토큰 무효화: 사용자 ID={}", userId);
        }
    }

    /**
     * 토큰이 Redis에 존재하는지 확인합니다.
     * 
     * @param token JWT 토큰
     * @return 토큰이 존재하면 true, 아니면 false
     */
    public boolean isTokenValid(String token) {
        String userKey = USER_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(userKey));
    }
}