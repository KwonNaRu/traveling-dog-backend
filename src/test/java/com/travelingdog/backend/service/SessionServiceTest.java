package com.travelingdog.backend.service;

import com.travelingdog.backend.model.User;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class SessionServiceTest {

    private static final String TEST_TOKEN = "test-jwt-token";
    private static final long TEST_EXPIRATION = 3600;
    private static final String TOKEN_PREFIX = "token:";
    private static final String USER_PREFIX = "user:";

    @Nested
    @ExtendWith(MockitoExtension.class)
    class SaveTokenTest {
        @Mock
        private RedisTemplate<String, Object> redisTemplate;

        @Mock
        private ValueOperations<String, Object> valueOperations;

        @InjectMocks
        private SessionService sessionService;

        @Test
        void saveToken_ShouldStoreTokenInRedis() {
            // Given
            User testUser = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .nickname("testUser")
                    .build();

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            // When
            sessionService.saveToken(testUser, TEST_TOKEN, TEST_EXPIRATION);

            // Then
            verify(valueOperations).set(TOKEN_PREFIX + testUser.getId(), TEST_TOKEN);
            verify(redisTemplate).expire(TOKEN_PREFIX + testUser.getId(), TEST_EXPIRATION, TimeUnit.SECONDS);

            verify(valueOperations).set(USER_PREFIX + TEST_TOKEN, testUser.getId());
            verify(redisTemplate).expire(USER_PREFIX + TEST_TOKEN, TEST_EXPIRATION, TimeUnit.SECONDS);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class GetUserIdByTokenTest {
        @Mock
        private RedisTemplate<String, Object> redisTemplate;

        @Mock
        private ValueOperations<String, Object> valueOperations;

        @InjectMocks
        private SessionService sessionService;

        @Test
        void getUserIdByToken_ShouldReturnUserId() {
            // Given
            Long userId = 1L;
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(USER_PREFIX + TEST_TOKEN)).thenReturn(userId);

            // When
            Long result = sessionService.getUserIdByToken(TEST_TOKEN);

            // Then
            assertThat(result).isEqualTo(userId);
            verify(valueOperations).get(USER_PREFIX + TEST_TOKEN);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class GetTokenByUserIdTest {
        @Mock
        private RedisTemplate<String, Object> redisTemplate;

        @Mock
        private ValueOperations<String, Object> valueOperations;

        @InjectMocks
        private SessionService sessionService;

        @Test
        void getTokenByUserId_ShouldReturnToken() {
            // Given
            Long userId = 1L;
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(TOKEN_PREFIX + userId)).thenReturn(TEST_TOKEN);

            // When
            String token = sessionService.getTokenByUserId(userId);

            // Then
            assertThat(token).isEqualTo(TEST_TOKEN);
            verify(valueOperations).get(TOKEN_PREFIX + userId);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class InvalidateTokenTest {
        @Mock
        private RedisTemplate<String, Object> redisTemplate;

        @Mock
        private ValueOperations<String, Object> valueOperations;

        @InjectMocks
        private SessionService sessionService;

        @Test
        void invalidateToken_ShouldDeleteTokenFromRedis() {
            // Given
            Long userId = 1L;
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(USER_PREFIX + TEST_TOKEN)).thenReturn(userId);

            // When
            sessionService.invalidateToken(TEST_TOKEN);

            // Then
            verify(valueOperations).get(USER_PREFIX + TEST_TOKEN);
            verify(redisTemplate).delete(TOKEN_PREFIX + userId);
            verify(redisTemplate).delete(USER_PREFIX + TEST_TOKEN);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class InvalidateAllUserTokensTest {
        @Mock
        private RedisTemplate<String, Object> redisTemplate;

        @Mock
        private ValueOperations<String, Object> valueOperations;

        @InjectMocks
        private SessionService sessionService;

        @Test
        void invalidateAllUserTokens_ShouldDeleteAllUserTokens() {
            // Given
            Long userId = 1L;
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(TOKEN_PREFIX + userId)).thenReturn(TEST_TOKEN);

            // When
            sessionService.invalidateAllUserTokens(userId);

            // Then
            verify(valueOperations).get(TOKEN_PREFIX + userId);
            verify(redisTemplate).delete(TOKEN_PREFIX + userId);
            verify(redisTemplate).delete(USER_PREFIX + TEST_TOKEN);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class IsTokenValidTest {
        @Mock
        private RedisTemplate<String, Object> redisTemplate;

        @InjectMocks
        private SessionService sessionService;

        @Test
        void isTokenValid_WithValidToken_ShouldReturnTrue() {
            // Given
            String userKey = USER_PREFIX + TEST_TOKEN;
            when(redisTemplate.hasKey(userKey)).thenReturn(true);

            // When
            boolean isValid = sessionService.isTokenValid(TEST_TOKEN);

            // Then
            assertThat(isValid).isTrue();
            verify(redisTemplate).hasKey(userKey);
        }

        @Test
        void isTokenValid_WithInvalidToken_ShouldReturnFalse() {
            // Given
            String userKey = USER_PREFIX + TEST_TOKEN;
            when(redisTemplate.hasKey(userKey)).thenReturn(false);

            // When
            boolean isValid = sessionService.isTokenValid(TEST_TOKEN);

            // Then
            assertThat(isValid).isFalse();
            verify(redisTemplate).hasKey(userKey);
        }
    }
}