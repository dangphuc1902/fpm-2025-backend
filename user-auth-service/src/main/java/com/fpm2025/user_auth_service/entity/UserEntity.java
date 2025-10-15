package com.fpm2025.user_auth_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_email", columnList = "email"),
        @Index(name = "idx_users_google_id", columnList = "google_id"),
        @Index(name = "idx_users_username", columnList = "username"),
        @Index(name = "idx_users_last_login", columnList = "last_login")
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

    @Column(name = "google_id", unique = true, length = 255)
    private String googleId;

    @Column(unique = true, length = 100)
    private String username;

    @Column(name = "hashed_password", length = 255)
    private String hashedPassword;

    @Column(columnDefinition = "TEXT")
    private String jwtToken;

    @Column(name = "jwt_token_encrypted", columnDefinition = "TEXT")
    private String jwtTokenEncrypted;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime updatedAt;

    @Column(name = "last_login", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime lastLogin;
}