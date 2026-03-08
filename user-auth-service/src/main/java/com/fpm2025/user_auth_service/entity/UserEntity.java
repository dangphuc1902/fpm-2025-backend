package com.fpm2025.user_auth_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_email", columnList = "email"),
        @Index(name = "idx_users_google_id", columnList = "google_id"),
        @Index(name = "idx_users_username", columnList = "username"),
        @Index(name = "idx_users_last_login", columnList = "last_login"),
        @Index(name = "idx_users_role", columnList = "role")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "phone_number", unique = true, length = 20)
    private String phoneNumber;

    @Column(name = "google_id", unique = true, length = 255)
    private String googleId;

    @Column(unique = true, length = 100)
    private String username;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "hashed_password", length = 255)
    private String hashedPassword;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String role = "USER";

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    @Column(name = "last_login", columnDefinition = "TIMESTAMP NULL")
    private LocalDateTime lastLogin;
}