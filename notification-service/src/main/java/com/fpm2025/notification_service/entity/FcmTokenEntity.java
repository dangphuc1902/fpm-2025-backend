package com.fpm2025.notification_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity lưu Firebase Cloud Messaging tokens.
 * Table: fcm_tokens (notification_db)
 */
@Entity
@Table(name = "fcm_tokens",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "device_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FcmTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "fcm_token", nullable = false, length = 500)
    private String fcmToken;

    /** ANDROID | IOS | WEB */
    @Column(name = "device_type", nullable = false, length = 10)
    @Builder.Default
    private String deviceType = "ANDROID";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
