package com.fpm2025.user_auth_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginAttemptServiceTest {

    private static final String KEY = "login_fail:1.2.3.4:a@test.local";

    private StringRedisTemplate redis;
    private ValueOperations<String, String> values;
    private LoginAttemptService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        service = new LoginAttemptService(redis);
    }

    @Test
    void isBlocked_falseBelowFiveFailures() {
        when(values.get(KEY)).thenReturn("4");

        assertFalse(service.isBlocked("1.2.3.4", "A@test.local"));
    }

    @Test
    void isBlocked_trueAtFiveFailures() {
        when(values.get(KEY)).thenReturn("5");

        assertTrue(service.isBlocked("1.2.3.4", "a@test.local"));
    }

    @Test
    void recordFailure_startsFiveMinuteWindowOnFirstFailure() {
        when(values.increment(KEY)).thenReturn(1L);

        service.recordFailure("1.2.3.4", "a@test.local");

        verify(redis).expire(KEY, Duration.ofMinutes(5));
    }

    @Test
    void recordFailure_doesNotExtendWindowOnLaterFailures() {
        when(values.increment(KEY)).thenReturn(2L);

        service.recordFailure("1.2.3.4", "a@test.local");

        verify(redis, never()).expire(anyString(), org.mockito.ArgumentMatchers.any(Duration.class));
    }

    @Test
    void reset_clearsFailures() {
        service.reset("1.2.3.4", "a@test.local");

        verify(redis).delete(KEY);
    }

    @Test
    void isBlocked_allowsLoginWhenRedisIsDown() {
        when(values.get(KEY)).thenThrow(new RedisConnectionFailureException("down"));

        assertFalse(service.isBlocked("1.2.3.4", "a@test.local"));
    }
}
