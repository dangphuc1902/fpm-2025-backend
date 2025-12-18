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
@Table(name = "monthly_summaries", indexes = {
    @Index(name = "idx_user_month", columnList = "user_id,month_start"),
    @Index(name = "idx_wallet_month", columnList = "wallet_id,month_start")
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
    
    @Column(name = "wallet_id")
    private Long walletId;
    
    @Column(name = "month_start", nullable = false)
    private LocalDate monthStart;
    
    @Column(name = "total_income", precision = 19, scale = 2)
    private BigDecimal totalIncome = BigDecimal.ZERO;
    
    @Column(name = "total_expense", precision = 19, scale = 2)
    private BigDecimal totalExpense = BigDecimal.ZERO;
    
    @Column(name = "net_savings", precision = 19, scale = 2)
    private BigDecimal netSavings = BigDecimal.ZERO;
    
    @Column(name = "transaction_count")
    private Integer transactionCount = 0;
    
    @Column(name = "avg_transaction_amount", precision = 19, scale = 2)
    private BigDecimal avgTransactionAmount = BigDecimal.ZERO;
    
    @Column(name = "opening_balance", precision = 19, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;
    
    @Column(name = "closing_balance", precision = 19, scale = 2)
    private BigDecimal closingBalance = BigDecimal.ZERO;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        calculateNetSavings();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateNetSavings();
    }
    
    private void calculateNetSavings() {
        this.netSavings = totalIncome.subtract(totalExpense);
    }
}