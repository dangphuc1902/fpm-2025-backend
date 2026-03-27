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
public class FamilyMemberResponse {
    private Long userId;
    private String email;
    private String username;
    private String avatarUrl;
    private String role;
    private LocalDateTime joinedAt;
}
