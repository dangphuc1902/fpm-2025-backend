package com.fpm2025.user_auth_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

/**
 * Gioi han dang nhap sai: 5 lan / 5 phut theo cap IP + email.
 * Khong tinh rieng theo IP vi nhieu may co the chung mot IP (NAT, Docker Desktop),
 * mot nguoi nhap sai se khoa ca nhom. Redis loi thi van cho dang nhap.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {

    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;

    public boolean isBlocked(String ip, String email) {
        try {
            String failures = redisTemplate.opsForValue().get(key(ip, email));
            return failures != null && Integer.parseInt(failures) >= MAX_FAILURES;
        } catch (RuntimeException e) {
            log.warn("Cannot read login attempts: {}", e.getMessage());
            return false;
        }
    }

    public void recordFailure(String ip, String email) {
        try {
            String key = key(ip, email);
            Long failures = redisTemplate.opsForValue().increment(key);
            if (failures != null && failures == 1L) {
                redisTemplate.expire(key, WINDOW);
            }
        } catch (RuntimeException e) {
            log.warn("Cannot record login failure: {}", e.getMessage());
        }
    }

    public void reset(String ip, String email) {
        try {
            redisTemplate.delete(key(ip, email));
        } catch (RuntimeException e) {
            log.warn("Cannot reset login attempts: {}", e.getMessage());
        }
    }

    private String key(String ip, String email) {
        return "login_fail:" + ip + ":" + email.trim().toLowerCase(Locale.ROOT);
    }
}
