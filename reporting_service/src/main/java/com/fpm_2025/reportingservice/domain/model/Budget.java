package com.fpm_2025.reportingservice.domain.model;

import com.fpm_2025.reportingservice.domain.valueobject.BudgetPeriod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "budgets", indexes = {
    @Index(name = "idx_user_category_period", columnList = "user_id,category_id,period"),
    @Index(name = "idx_active_budgets", columnList = "is_active,start_date,end_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Budget {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "category_id")
    private Long categoryId; // null = overall budget
    
    @Column(name = "wallet_id")
    private Long walletId; // null = all wallets
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Column(name = "amount_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal amountLimit;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BudgetPeriod period;
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "alert_threshold", precision = 5, scale = 2)
    private BigDecimal alertThreshold = BigDecimal.valueOf(80); // 80%
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "current_spent", precision = 19, scale = 2)
    private BigDecimal currentSpent = BigDecimal.ZERO;
    
    @Column(name = "rollover_enabled")
    private Boolean rolloverEnabled = false;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public BigDecimal getRemainingAmount() {
        return amountLimit.subtract(currentSpent);
    }
    
    public BigDecimal getUsagePercentage() {
        if (amountLimit.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return currentSpent.divide(amountLimit, 4, BigDecimal.ROUND_HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }
    
    public boolean isOverBudget() {
        return currentSpent.compareTo(amountLimit) > 0;
    }
    
    public boolean shouldAlert() {
        return getUsagePercentage().compareTo(alertThreshold) >= 0;
    }
    
    public boolean isCurrentPeriod() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(startDate) && 
               (endDate == null || !today.isAfter(endDate));
    }
}