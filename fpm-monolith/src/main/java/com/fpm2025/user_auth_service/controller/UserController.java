package com.fpm2025.user_auth_service.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fpm2025.user_auth_service.entity.UserEntity;
import com.fpm2025.user_auth_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestHeader("X-User-Id") String userIdStr) {
        Long userId = Long.parseLong(userIdStr);
        Optional<UserEntity> userOpt = userRepository.findById(userId.intValue());
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        UserEntity user = userOpt.get();
        return ResponseEntity.ok(mapToResponse(user));
    }

    @GetMapping("/find-by-email")
    public ResponseEntity<Map<String, Object>> findByEmail(@org.springframework.web.bind.annotation.RequestParam String email) {
        return userRepository.findByEmail(email)
                .map(user -> ResponseEntity.ok(mapToResponse(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/me")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestHeader("X-User-Id") String userIdStr,
            @RequestBody Map<String, String> request) {

        Long userId = Long.parseLong(userIdStr);
        Optional<UserEntity> userOpt = userRepository.findById(userId.intValue());
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        UserEntity user = userOpt.get();
        if (request.containsKey("username")) {
            user.setUsername(request.get("username"));
        }
        if (request.containsKey("avatar")) {
            user.setAvatarUrl(request.get("avatar"));
        }

        userRepository.save(user);
        return ResponseEntity.ok(mapToResponse(user));
    }

    private Map<String, Object> mapToResponse(UserEntity user) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("username", user.getUsername());
        response.put("avatar", user.getAvatarUrl());
        response.put("created_at", user.getCreatedAt());
        return response;
    }
}
