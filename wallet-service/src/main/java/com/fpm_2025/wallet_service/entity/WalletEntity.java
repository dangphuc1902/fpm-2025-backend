package com.fpm_2025.wallet_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import com.fpm_2025.wallet_service.entity.enums.WalletType;
@Setter
@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "wallets", indexes = {
        @Index(name = "idx_wallets_user_id", columnList = "user_id"),
        @Index(name = "idx_wallets_type", columnList = "type"),
        @Index(name = "idx_wallets_is_active", columnList = "is_active")
})
@Builder
public class WalletEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id",nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletType type;
    
    @Column(nullable = false)
    private String icon;
    
    @Column(name = "is_active")
    private boolean isActive;
    
    @Column(updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now();
    
    @Column
    private ZonedDateTime updatedAt = ZonedDateTime.now();

}