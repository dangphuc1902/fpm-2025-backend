package com.fpm2025.user_auth_service.service;

import com.fpm2025.domain.event.UserCreatedEvent;
import com.fpm2025.user_auth_service.entity.UserEntity;
import com.fpm2025.user_auth_service.payload.request.UserLoginRequest;
import com.fpm2025.user_auth_service.payload.request.UserRegisterRequest;
import com.fpm2025.user_auth_service.repository.UserRepository;
import com.fpm2025.security.jwt.JwtTokenProvider;
import com.fpm2025.user_auth_service.exception.UserAlreadyExistsException;
import com.fpm2025.user_auth_service.exception.UserEmailNotExistException;
import com.fpm2025.user_auth_service.exception.InvalidPasswordException;
import com.fpm2025.user_auth_service.payload.request.RefreshTokenRequest;
import com.fpm2025.user_auth_service.entity.RefreshToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Authentication Service
 * 
 * Features:
 * - Email/Password authentication
 * - Google OAuth2 authentication
 * - JWT token generation & validation
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final WebClient.Builder webClientBuilder;
    private final JwtBlacklistService jwtBlacklistService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final RefreshTokenService refreshTokenService;

    @org.springframework.beans.factory.annotation.Autowired
    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            WebClient.Builder webClientBuilder,
            JwtBlacklistService jwtBlacklistService,
            org.springframework.context.ApplicationEventPublisher eventPublisher,
            RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.webClientBuilder = webClientBuilder;
        this.jwtBlacklistService = jwtBlacklistService;
        this.eventPublisher = eventPublisher;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public Map<String, Object> login(UserLoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        UserEntity user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new UserEmailNotExistException(
                "User not found with email: " + request.getEmail()));

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getHashedPassword())) {
            throw new InvalidPasswordException("Invalid password");
        }

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Generate JWT token
        String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), null);

        log.info("Login successful for user: {}", user.getEmail());

        return buildAuthResponse(user, token);
    }

    @Transactional
    public Map<String, Object> register(UserRegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());

        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(
                "User already exists with email: " + request.getEmail());
        }

        // Create new user
        UserEntity user = UserEntity.builder()
            .email(request.getEmail())
            .username(request.getUsername())
            .hashedPassword(passwordEncoder.encode(request.getPassword()))
            .build();

        UserEntity savedUser = userRepository.save(user);

        // Generate JWT token
        String token = jwtTokenProvider.generateAccessToken(savedUser.getId(), savedUser.getEmail(), null);

        // Publish user.created event → wallet-service tạo ví mặc định
        publishUserCreatedEvent(savedUser);

        log.info("Registration successful for user: {}", savedUser.getEmail());

        return buildAuthResponse(savedUser, token);
    }

    @Transactional
    public Map<String, Object> loginWithGoogle(String googleToken) {
        log.info("Google login attempt");

        try {
            // Verify Google token và lấy user info
            Map<String, Object> googleUserInfo = verifyGoogleToken(googleToken);
            
            String email = (String) googleUserInfo.get("email");
            String name = (String) googleUserInfo.get("name");
            String googleId = (String) googleUserInfo.get("sub");
            String picture = (String) googleUserInfo.get("picture");

            // Tìm hoặc tạo user
            UserEntity user = userRepository.findByEmail(email)
                .orElseGet(() -> createGoogleUser(email, name, googleId, picture));

            // Update Google ID nếu chưa có
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
            }

            // Update last login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Generate JWT token
            String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), null);

            log.info("Google login successful for user: {}", user.getEmail());

            return buildAuthResponse(user, token);

        } catch (Exception e) {
            log.error("Google login failed", e);
            throw new RuntimeException("Google login failed: " + e.getMessage());
        }
    }

    public Map<String, Object> validateToken(String token) {
        log.info("Token validation request");

        try {
            if (jwtBlacklistService.isTokenBlacklisted(token)) {
                throw new RuntimeException("Token is blacklisted");
            }
            Long userId = jwtTokenProvider.extractUserId(token);
            String email = jwtTokenProvider.extractEmail(token);

            UserEntity user = userRepository.findById(Math.toIntExact(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("userId", userId);
            response.put("email", email);
            response.put("username", user.getUsername());

            return response;

        } catch (Exception e) {
            log.error("Token validation failed", e);
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    @Transactional
    public void logout(String token) {
        log.info("Logout request");
        Long userId = jwtTokenProvider.extractUserId(token);
        long remainingTime = jwtTokenProvider.getRemainingExpiration(token);
        jwtBlacklistService.blacklistToken(token, remainingTime);
        log.info("User {} logged out, token blacklisted.", userId);
    }

    // ==================== Private Methods ====================

    private Map<String, Object> verifyGoogleToken(String googleToken) {
        log.info("Verifying Google token");

        // Call Google's token info endpoint
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + googleToken;

        return webClientBuilder.build()
            .get()
            .uri(url)
            .retrieve()
            .bodyToMono(Map.class)
            .block();
    }

    private UserEntity createGoogleUser(
            String email, 
            String name, 
            String googleId, 
            String picture) {
        
        log.info("Creating new user from Google: {}", email);

        UserEntity user = UserEntity.builder()
            .email(email)
            .username(name)
            .googleId(googleId)
            // No password for Google users
            .build();

        UserEntity saved = userRepository.save(user);

        // Publish user.created event → wallet-service tạo ví mặc định
        publishUserCreatedEvent(saved);

        return saved;
    }

    private Map<String, Object> buildAuthResponse(UserEntity user, String token) {
        Map<String, Object> response = new HashMap<>();
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("email", user.getEmail());
        userInfo.put("username", user.getUsername());
        userInfo.put("googleId", user.getGoogleId());

        // Delete any existing refresh tokens and create a new one
        refreshTokenService.deleteByUserId(user.getId());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        response.put("token", token);
        response.put("refreshToken", refreshToken.getToken());
        response.put("user", userInfo);
        response.put("expiresIn", jwtTokenProvider.getExpiration());

        return response;
    }

    /**
     * Publish local Spring Event 'user.created' sau khi user đăng ký thành công.
     */
    private void publishUserCreatedEvent(UserEntity user) {
        try {
            UserCreatedEvent event = UserCreatedEvent.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .createdAt(user.getCreatedAt() != null
                            ? user.getCreatedAt().toString()
                            : LocalDateTime.now().toString())
                    .build();

            eventPublisher.publishEvent(event);
            log.info("Local SpringEvent: Published UserCreatedEvent for userId={} email={}",
                    user.getId(), user.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish UserCreatedEvent for userId={}: {}",
                    user.getId(), e.getMessage(), e);
        }
    }

    @Transactional
    public Map<String, Object> refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();
        if (requestRefreshToken == null || requestRefreshToken.isBlank()) {
            throw new RuntimeException("Refresh token is missing or empty");
        }

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), null);
                    
                    // Rotate refresh token
                    refreshTokenService.deleteByUserId(user.getId());
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("token", accessToken);
                    response.put("refreshToken", newRefreshToken.getToken());
                    response.put("expiresIn", jwtTokenProvider.getExpiration());
                    
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", user.getId());
                    userInfo.put("email", user.getEmail());
                    userInfo.put("username", user.getUsername());
                    userInfo.put("googleId", user.getGoogleId());
                    response.put("user", userInfo);
                    
                    log.info("Token refreshed successfully for user ID: {}", user.getId());
                    return response;
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database"));
    }
}