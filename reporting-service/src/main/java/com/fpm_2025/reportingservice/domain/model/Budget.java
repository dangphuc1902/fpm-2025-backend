package com.fpm_2025.reportingservice.domain.model;

import com.fpm_2025.reportingservice.domain.valueobject.BudgetPeriod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "budgets", indexes = {
    @Index(name = "idx_budgets_user_month", columnList = "user_id, year_month"),
    @Index(name = "idx_budgets_category", columnList = "category_id")
},
uniqueConstraints = {
    @UniqueConstraint(name = "uk_budget_user_cat_month", columnNames = {"user_id", "category_id", "year_month"})
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
    
    @Column(name = "category_id", nullable = false)
    private Long categoryId;
    
    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;
    
    @Column(name = "amount_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountLimit;
    
    @Builder.Default
    @Column(name = "amount_used", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountUsed = BigDecimal.ZERO;
    
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false, length = 20)
    private BudgetPeriod period = BudgetPeriod.MONTHLY;
    
    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;
    
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_at")
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
        return amountLimit.subtract(amountUsed);
    }
    
    public BigDecimal getUsagePercentage() {
        if (amountLimit.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return amountUsed.divide(amountLimit, 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }
    
    public boolean isOverBudget() {
        return amountUsed.compareTo(amountLimit) > 0;
    }
}