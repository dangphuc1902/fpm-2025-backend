package com.fpm2025.user_auth_service.service;

import com.fpm2025.user_auth_service.entity.UserEntity;
import com.fpm2025.user_auth_service.payload.request.UserLoginRequest;
import com.fpm2025.user_auth_service.payload.request.UserRegisterRequest;
import com.fpm2025.user_auth_service.repository.UserRepository;
import com.fpm2025.user_auth_service.util.JwtUtils;
import com.fpm2025.user_auth_service.exception.UserAlreadyExistsException;
import com.fpm2025.user_auth_service.exception.UserEmailNotExistException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
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
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final WebClient.Builder webClientBuilder;

    @Transactional
    public Map<String, Object> login(UserLoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        UserEntity user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new UserEmailNotExistException(
                "User not found with email: " + request.getEmail()));

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getHashedPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // Update last login
        user.setLastLogin(OffsetDateTime.now());
        userRepository.save(user);

        // Generate JWT token
        String token = jwtUtils.generateToken(user.getId(), user.getEmail());

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
            .username(request.getFullName())
            .hashedPassword(passwordEncoder.encode(request.getPassword()))
            .build();

        UserEntity savedUser = userRepository.save(user);

        // Generate JWT token
        String token = jwtUtils.generateToken(savedUser.getId(), savedUser.getEmail());

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
            user.setLastLogin(OffsetDateTime.now());
            userRepository.save(user);

            // Generate JWT token
            String token = jwtUtils.generateToken(user.getId(), user.getEmail());

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
            Long userId = jwtUtils.extractUserId(token);
            String email = jwtUtils.extractEmail(token);

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
    public void logout(Long userId) {
        log.info("Logout request for user: {}", userId);

        UserEntity user = userRepository.findById(Math.toIntExact(userId))
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Clear JWT token (nếu store trong DB)
        user.setJwtToken(null);
        user.setJwtTokenEncrypted(null);
        userRepository.save(user);

        log.info("Logout successful for user: {}", userId);
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

        return userRepository.save(user);
    }

    private Map<String, Object> buildAuthResponse(UserEntity user, String token) {
        Map<String, Object> response = new HashMap<>();
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("email", user.getEmail());
        userInfo.put("username", user.getUsername());
        userInfo.put("googleId", user.getGoogleId());

        response.put("token", token);
        response.put("user", userInfo);
        response.put("expiresIn", jwtUtils.getExpiration());

        return response;
    }
}