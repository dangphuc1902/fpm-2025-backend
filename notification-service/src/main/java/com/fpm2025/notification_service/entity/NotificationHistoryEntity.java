package com.fpm2025.notification_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity lưu lịch sử thông báo gửi đến user.
 * Table: notification_history (notification_db)
 */
@Entity
@Table(name = "notification_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    /** TRANSACTION | BUDGET_ALERT | FAMILY_INVITE | SYSTEM */
    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    /** FCM | EMAIL | SMS */
    @Column(name = "sent_via", nullable = false, length = 10)
    @Builder.Default
    private String sentVia = "FCM";

    /** PENDING | SENT | FAILED */
    @Column(nullable = false, length = 10)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
