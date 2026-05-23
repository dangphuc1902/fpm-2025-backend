package com.fpm_2025.reportingservice.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "monthly_summaries", indexes = {
    @Index(name = "idx_monthly_user_id", columnList = "user_id"),
    @Index(name = "idx_monthly_year_month", columnList = "year_month")
},
uniqueConstraints = {
    @UniqueConstraint(name = "uk_monthly_user_month", columnNames = {"user_id", "year_month"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySummary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;
    
    @Builder.Default
    @Column(name = "total_income", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalIncome = BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "total_expense", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalExpense = BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "net_income", nullable = false, precision = 15, scale = 2)
    private BigDecimal netIncome = BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "transaction_count", nullable = false)
    private Integer transactionCount = 0;
    
    @Column(name = "avg_daily_expense", precision = 15, scale = 2)
    private BigDecimal avgDailyExpense;
    
    @Column(name = "top_expense_category", length = 100)
    private String topExpenseCategory;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        calculateNetIncome();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateNetIncome();
    }
    
    private void calculateNetIncome() {
        if (totalIncome != null && totalExpense != null) {
            this.netIncome = totalIncome.subtract(totalExpense);
        }
    }
}