package com.fpm_2025.wallet_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fpm2025.domain.enums.WalletType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "wallets", indexes = {
        @Index(name = "idx_wallets_user_id", columnList = "user_id"),
        @Index(name = "idx_wallets_type", columnList = "type"),
        @Index(name = "idx_wallets_active", columnList = "is_active"),
        @Index(name = "idx_wallets_deleted", columnList = "is_deleted"),
        @Index(name = "idx_wallets_user_active", columnList = "user_id, is_active, is_deleted")
})
@org.hibernate.annotations.Where(clause = "is_deleted = false")
public class WalletEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "family_id")
    private Long familyId;

    @Column(nullable = false, length = 100)
    private String name;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletType type = WalletType.CASH;

    @Builder.Default
    @Column(length = 3, nullable = false)
    private String currency = "VND";

    @Column(name = "currency_symbol", length = 5)
    @Builder.Default
    private String currencySymbol = "₫";

    @Builder.Default
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(length = 50)
    private String icon;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
