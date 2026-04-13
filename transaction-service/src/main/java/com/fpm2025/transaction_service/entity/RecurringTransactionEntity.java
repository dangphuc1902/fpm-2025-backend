package com.fpm2025.transaction_service.entity;

import com.fpm2025.domain.enums.CategoryType;
import com.fpm2025.transaction_service.entity.enums.RecurringFrequency;
import com.fpm2025.transaction_service.entity.enums.RecurringStatus;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recurring_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    @Builder.Default
    private String currency = "VND";

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CategoryType type;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private RecurringFrequency frequency;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "last_processed_date")
    private LocalDateTime lastProcessedDate;

    @Column(name = "next_process_date", nullable = false)
    private LocalDateTime nextProcessDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RecurringStatus status = RecurringStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
