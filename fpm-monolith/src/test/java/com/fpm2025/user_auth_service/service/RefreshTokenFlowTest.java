package com.fpm2025.user_auth_service.service;

import com.fpm2025.security.jwt.JwtTokenProvider;
import com.fpm2025.user_auth_service.entity.RefreshToken;
import com.fpm2025.user_auth_service.entity.UserEntity;
import com.fpm2025.user_auth_service.exception.InvalidRefreshTokenException;
import com.fpm2025.user_auth_service.payload.request.RefreshTokenRequest;
import com.fpm2025.user_auth_service.repository.RefreshTokenRepository;
import com.fpm2025.user_auth_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenFlowTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private AuthService authService() {
        return new AuthService(userRepository, mock(PasswordEncoder.class), mock(JwtTokenProvider.class),
                mock(WebClient.Builder.class), mock(JwtBlacklistService.class),
                mock(ApplicationEventPublisher.class), refreshTokenService);
    }

    private RefreshTokenRequest request(String token) {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(token);
        return request;
    }

    private RefreshToken token(Instant expiresAt) {
        UserEntity user = new UserEntity();
        user.setId(1L);
        return RefreshToken.builder().user(user).token("rt").expiresAt(expiresAt).build();
    }

    @Test
    void refresh_rejectsMissingToken() {
        assertThrows(InvalidRefreshTokenException.class, () -> authService().refreshToken(request(" ")));
    }

    @Test
    void refresh_rejectsUnknownToken() {
        when(refreshTokenRepository.findByToken("rt")).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class, () -> authService().refreshToken(request("rt")));
    }

    @Test
    void refresh_rejectsExpiredToken() {
        when(refreshTokenRepository.findByToken("rt")).thenReturn(Optional.of(token(Instant.now().minusSeconds(60))));

        assertThrows(InvalidRefreshTokenException.class, () -> authService().refreshToken(request("rt")));
    }

    @Test
    void refresh_rejectsTokenAlreadyConsumedByConcurrentRequest() {
        when(refreshTokenRepository.findByToken("rt")).thenReturn(Optional.of(token(Instant.now().plusSeconds(60))));
        when(refreshTokenRepository.consumeToken("rt")).thenReturn(0);

        assertThrows(InvalidRefreshTokenException.class, () -> authService().refreshToken(request("rt")));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void deleteByUserId_deletesWithBulkQueryInsteadOfManagedCollection() {
        refreshTokenService.deleteByUserId(1L);

        verify(refreshTokenRepository).deleteAllByUserId(1L);
    }
}
