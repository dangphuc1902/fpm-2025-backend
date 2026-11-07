package com.fpm2025.user_auth_service.service;

import com.fpm2025.user_auth_service.entity.RefreshToken;
import com.fpm2025.user_auth_service.entity.UserEntity;
import com.fpm2025.user_auth_service.exception.InvalidRefreshTokenException;
import com.fpm2025.user_auth_service.repository.RefreshTokenRepository;
import com.fpm2025.user_auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${app.jwt.refreshTokenExpirationMs}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        UserEntity user = userRepository.findById(Math.toIntExact(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(Instant.now().plusMillis(refreshTokenDurationMs))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new InvalidRefreshTokenException("Refresh token was expired. Please make a new signin request");
        }
        if(token.isRevoked()){
            throw new InvalidRefreshTokenException("Refresh token was revoked. Please make a new signin request");
        }
        return token;
    }

    /** @return false neu request khac da dung token nay truoc */
    @Transactional
    public boolean consume(String token) {
        return refreshTokenRepository.consumeToken(token) > 0;
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        refreshTokenRepository.deleteAllByUserId(userId);
    }
}
