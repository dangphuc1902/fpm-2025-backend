package com.fpm2025.user_auth_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferencesEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(length = 10)
    private String language;

    @Column(length = 10)
    private String currency;

    @Column(length = 20)
    private String theme;
}
