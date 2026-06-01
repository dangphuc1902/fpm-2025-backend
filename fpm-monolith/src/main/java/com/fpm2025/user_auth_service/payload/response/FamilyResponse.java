package com.fpm2025.user_auth_service.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private String currentUserRole;
}
