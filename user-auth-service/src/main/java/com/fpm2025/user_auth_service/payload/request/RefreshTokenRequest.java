package com.fpm2025.user_auth_service.payload.request;

import lombok.Data;

@Data
public class RefreshTokenRequest {
    private String refreshToken;
}
