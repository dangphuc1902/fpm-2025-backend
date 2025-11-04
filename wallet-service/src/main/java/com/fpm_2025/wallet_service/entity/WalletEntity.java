package com.fpm_2025.wallet_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fpm_2025.wallet_service.entity.enums.WalletType;

@Getter
@Setter
@AllArgsConstructor
@Builder
@Entity
@Table(name = "wallets", indexes = {
        @Index(name = "idx_wallets_user_id", columnList = "user_id"),
        @Index(name = "idx_wallets_type", columnList = "type"),
        @Index(name = "idx_wallets_is_active", columnList = "is_active")
})
public class WalletEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;
    
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "wallet_type DEFAULT 'cash'")
    private WalletType type = WalletType.CASH;

    @Column(length = 3)
    private String currency = "VND";
    
    @Builder.Default
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(length = 50)
    private String icon;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
    
    @Builder.Default
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
