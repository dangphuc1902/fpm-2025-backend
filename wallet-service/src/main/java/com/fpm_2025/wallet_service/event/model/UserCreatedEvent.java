package com.fpm_2025.wallet_service.event.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Event received from user-auth-service via Kafka mapping
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreatedEvent implements Serializable {
    private Long userId;
    private String email;
    private String username;
    private String createdAt;
}
