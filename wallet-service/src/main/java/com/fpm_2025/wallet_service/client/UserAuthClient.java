package com.fpm_2025.wallet_service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserAuthClient {

    private final RestTemplate restTemplate;
    private static final String USER_SERVICE_URL = "http://user-auth-service/api/v1/users/find-by-email";

    /**
     * Lấy userId thông qua email bằng cách gọi API của user-auth-service.
     */
    public Long getUserIdByEmail(String email) {
        log.info("[UserAuthClient] Resolving userId for email: {}", email);
        try {
            String url = UriComponentsBuilder.fromHttpUrl(USER_SERVICE_URL)
                    .queryParam("email", email)
                    .toUriString();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> userData = (Map<String, Object>) response.get("data");
                if (userData != null && userData.containsKey("id")) {
                    return Long.valueOf(userData.get("id").toString());
                }
            }
            return null;
        } catch (Exception e) {
            log.error("[UserAuthClient] Error calling user-auth-service: {}", e.getMessage());
            return null;
        }
    }
}
