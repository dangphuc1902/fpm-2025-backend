package com.fpm2025.wallet_service.client;

import com.fpm2025.user_auth_service.repository.UserRepository;
import com.fpm2025.user_auth_service.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserAuthClient {

    private final UserRepository userRepository;

    /**
     * Lấy userId thông qua email bằng cách gọi trực tiếp UserRepository.
     */
    public Long getUserIdByEmail(String email) {
        log.info("[UserAuthClient] Resolving userId directly from UserRepository for email: {}", email);
        try {
            return userRepository.findByEmail(email)
                    .map(UserEntity::getId)
                    .orElse(null);
        } catch (Exception e) {
            log.error("[UserAuthClient] Error querying UserRepository: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Lấy UserEntity thông qua userId.
     */
    public UserEntity getUserById(Long userId) {
        if (userId == null) return null;
        log.info("[UserAuthClient] Resolving UserEntity directly from UserRepository for id: {}", userId);
        try {
            return userRepository.findById(userId.intValue()).orElse(null);
        } catch (Exception e) {
            log.error("[UserAuthClient] Error querying UserRepository for id {}: {}", userId, e.getMessage());
            return null;
        }
    }
}

