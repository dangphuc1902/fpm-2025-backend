package com.fpm_2025.transaction_service.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transactions_user_id", columnList = "user_id"),
    @Index(name = "idx_transactions_wallet_id", columnList = "wallet_id"),
    @Index(name = "idx_transactions_category_id", columnList = "category_id"),
    @Index(name = "idx_transactions_type", columnList = "type"),
    @Index(name = "idx_transactions_date", columnList = "transaction_date"),
    @Index(name = "idx_transactions_user_date", columnList = "user_id, transaction_date"),
    @Index(name = "idx_transactions_user_wallet", columnList = "user_id, wallet_id, transaction_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private String type;  // EXPENSE, INCOME

    @Column(name = "note", length = 255)
    private String note;

    @Builder.Default
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate = LocalDateTime.now();

    @Column(name = "location_json", columnDefinition = "JSON")
    private String locationJson;

    @Builder.Default
    @Column(name = "is_recurring")
    private Boolean isRecurring = false;

    @Column(name = "recurring_id")
    private Long recurringId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Amount validation
    @PrePersist
    @PreUpdate
    private void validateAmount() {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Amount must be greater than zero");
        }
    }
}