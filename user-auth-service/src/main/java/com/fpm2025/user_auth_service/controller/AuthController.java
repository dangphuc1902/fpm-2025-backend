package com.fpm2025.user_auth_service.controller;

import com.fpm2025.user_auth_service.payload.request.UserLoginRequest;
import com.fpm2025.user_auth_service.payload.request.UserRegisterRequest;
import com.fpm2025.user_auth_service.payload.response.BaseResponse;
import com.fpm2025.user_auth_service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication REST Controller
 * 
 * Endpoints:
 * - POST /api/v1/auth/register - User registration
 * - POST /api/v1/auth/login - Email/password login
 * - POST /api/v1/auth/google - Google OAuth2 login
 * - POST /api/v1/auth/validate - JWT token validation
 * - POST /api/v1/auth/logout - User logout
 * - POST /api/v1/auth/refresh - Refresh JWT token
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register new user")
    public ResponseEntity<BaseResponse<Map<String, Object>>> register(
            @Valid @RequestBody UserRegisterRequest request) {
        
        Map<String, Object> response = authService.register(request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Registration successful"));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<BaseResponse<Map<String, Object>>> login(
            @Valid @RequestBody UserLoginRequest request) {
        
        Map<String, Object> response = authService.login(request);
        
        return ResponseEntity.ok(
            BaseResponse.success(response, "Login successful")
        );
    }

    @PostMapping("/google")
    @Operation(summary = "Login with Google OAuth2")
    public ResponseEntity<BaseResponse<Map<String, Object>>> loginWithGoogle(
            @RequestParam String token) {
        
        Map<String, Object> response = authService.loginWithGoogle(token);
        
        return ResponseEntity.ok(
            BaseResponse.success(response, "Google login successful")
        );
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate JWT token")
    public ResponseEntity<BaseResponse<Map<String, Object>>> validateToken(
            @RequestHeader("Authorization") String authHeader) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.error("Invalid authorization header"));
        }

        String token = authHeader.substring(7);
        Map<String, Object> response = authService.validateToken(token);
        
        return ResponseEntity.ok(
            BaseResponse.success(response, "Token validation complete")
        );
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user")
    public ResponseEntity<BaseResponse<Void>> logout(
            @RequestParam Long userId) {
        
        authService.logout(userId);
        
        return ResponseEntity.ok(
            BaseResponse.success(null, "Logout successful")
        );
    }

    @GetMapping("/health")
    @Operation(summary = "Health check endpoint")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(
            Map.of(
                "status", "UP",
                "service", "user-auth-service"
            )
        );
    }
}