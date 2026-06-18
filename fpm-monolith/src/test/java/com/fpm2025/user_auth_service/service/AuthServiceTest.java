package com.fpm2025.user_auth_service.service;

import com.fpm2025.user_auth_service.entity.UserEntity;
import com.fpm2025.user_auth_service.exception.InvalidPasswordException;
import com.fpm2025.user_auth_service.exception.UserAlreadyExistsException;
import com.fpm2025.user_auth_service.exception.UserEmailNotExistException;
import com.fpm2025.user_auth_service.payload.request.UserLoginRequest;
import com.fpm2025.user_auth_service.payload.request.UserRegisterRequest;
import com.fpm2025.user_auth_service.repository.UserRepository;
import com.fpm2025.security.jwt.JwtTokenProvider;
import com.fpm2025.user_auth_service.entity.RefreshToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private JwtBlacklistService jwtBlacklistService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtTokenProvider,
                webClientBuilder,
                jwtBlacklistService,
                eventPublisher,
                refreshTokenService
        );
    }

    @Test
    void login_Success() {
        // Arrange
        UserLoginRequest request = new UserLoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        UserEntity user = UserEntity.builder()
                .id(1L)
                .email("test@example.com")
                .username("testuser")
                .hashedPassword("encodedPassword")
                .build();

        RefreshToken mockRefreshToken = new RefreshToken();
        mockRefreshToken.setToken("mock-refresh-token");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getHashedPassword())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(eq(1L), eq("test@example.com"), any())).thenReturn("mock-access-token");
        when(jwtTokenProvider.getExpiration()).thenReturn(3600L);
        when(refreshTokenService.createRefreshToken(1L)).thenReturn(mockRefreshToken);

        // Act
        Map<String, Object> response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals("mock-access-token", response.get("token"));
        assertEquals("mock-refresh-token", response.get("refreshToken"));
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void login_EmailNotFound_ThrowsException() {
        // Arrange
        UserLoginRequest request = new UserLoginRequest();
        request.setEmail("unknown@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserEmailNotExistException.class, () -> authService.login(request));
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void login_InvalidPassword_ThrowsException() {
        // Arrange
        UserLoginRequest request = new UserLoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        UserEntity user = UserEntity.builder()
                .id(1L)
                .email("test@example.com")
                .hashedPassword("encodedPassword")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getHashedPassword())).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidPasswordException.class, () -> authService.login(request));
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void register_Success() {
        // Arrange
        UserRegisterRequest request = new UserRegisterRequest();
        request.setEmail("register@example.com");
        request.setUsername("reguser");
        request.setPassword("password123");

        UserEntity savedUser = UserEntity.builder()
                .id(2L)
                .email("register@example.com")
                .username("reguser")
                .build();

        RefreshToken mockRefreshToken = new RefreshToken();
        mockRefreshToken.setToken("mock-refresh-token-2");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateAccessToken(eq(2L), eq("register@example.com"), any())).thenReturn("mock-access-token-2");
        when(jwtTokenProvider.getExpiration()).thenReturn(3600L);
        when(refreshTokenService.createRefreshToken(2L)).thenReturn(mockRefreshToken);

        // Act
        Map<String, Object> response = authService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals("mock-access-token-2", response.get("token"));
        assertEquals("mock-refresh-token-2", response.get("refreshToken"));
        verify(userRepository, times(1)).save(any(UserEntity.class));
    }

    @Test
    void register_UserAlreadyExists_ThrowsException() {
        // Arrange
        UserRegisterRequest request = new UserRegisterRequest();
        request.setEmail("existing@example.com");
        request.setUsername("existinguser");
        request.setPassword("password123");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(UserEntity.class));
    }
}
