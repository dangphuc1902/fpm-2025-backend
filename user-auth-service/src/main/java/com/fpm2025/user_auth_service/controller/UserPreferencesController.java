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

import com.fpm2025.user_auth_service.entity.UserPreferencesEntity;
import com.fpm2025.user_auth_service.repository.UserPreferencesRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users/preferences")
@RequiredArgsConstructor
public class UserPreferencesController {

    private final UserPreferencesRepository preferencesRepository;

    @GetMapping
    public ResponseEntity<Map<String, String>> getPreferences(@RequestHeader("X-User-Id") String userIdStr) {
        Long userId = Long.parseLong(userIdStr);
        Optional<UserPreferencesEntity> prefOpt = preferencesRepository.findByUserId(userId);

        Map<String, String> response = new HashMap<>();
        if (prefOpt.isPresent()) {
            UserPreferencesEntity pref = prefOpt.get();
            response.put("language", pref.getLanguage());
            response.put("currency", pref.getCurrency());
            response.put("theme", pref.getTheme());
        } else {
            response.put("language", null);
            response.put("currency", null);
            response.put("theme", null);
        }

        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<Map<String, String>> updatePreferences(
            @RequestHeader("X-User-Id") String userIdStr,
            @RequestBody Map<String, String> request) {

        Long userId = Long.parseLong(userIdStr);
        UserPreferencesEntity pref = preferencesRepository.findByUserId(userId)
                .orElse(UserPreferencesEntity.builder().userId(userId).build());

        if (request.containsKey("language")) {
            pref.setLanguage(request.get("language"));
        }
        if (request.containsKey("currency")) {
            pref.setCurrency(request.get("currency"));
        }
        if (request.containsKey("theme")) {
            pref.setTheme(request.get("theme"));
        }

        preferencesRepository.save(pref);

        Map<String, String> response = new HashMap<>();
        response.put("language", pref.getLanguage());
        response.put("currency", pref.getCurrency());
        response.put("theme", pref.getTheme());

        return ResponseEntity.ok(response);
    }
}
