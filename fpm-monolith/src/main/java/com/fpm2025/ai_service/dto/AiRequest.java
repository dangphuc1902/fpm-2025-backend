package com.fpm2025.ai_service.dto;

import lombok.Data;

@Data
public class AiRequest {
    private String text;
    private Long userId;
}
