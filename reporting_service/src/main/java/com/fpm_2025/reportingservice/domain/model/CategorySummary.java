package com.fpm_2025.reportingservice.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "category_summaries", indexes = {
    @Index(name = "idx_user_category_month", columnList = "user_id,category_id,month_start"),
    @Index(name = "idx_category_month", columnList = "category_id,month_start")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySummary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "category_id", nullable = false)
    private Long categoryId;
    
    @Column(name = "category_name")
    private String categoryName;
    
    @Column(name = "month_start", nullable = false)
    private LocalDate monthStart;
    
    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    
    @Column(name = "transaction_count")
    private Integer transactionCount = 0;
    
    @Column(name = "avg_amount", precision = 19, scale = 2)
    private BigDecimal avgAmount = BigDecimal.ZERO;
    
    @Column(name = "budget_limit", precision = 19, scale = 2)
    private BigDecimal budgetLimit;
    
    @Column(name = "percentage_of_budget", precision = 5, scale = 2)
    private BigDecimal percentageOfBudget;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        calculateAverages();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateAverages();
    }
    
    private void calculateAverages() {
        if (transactionCount > 0) {
            avgAmount = totalAmount.divide(
                BigDecimal.valueOf(transactionCount), 2, BigDecimal.ROUND_HALF_UP
            );
        }
        
        if (budgetLimit != null && budgetLimit.compareTo(BigDecimal.ZERO) > 0) {
            percentageOfBudget = totalAmount
                .divide(budgetLimit, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }
    }
}