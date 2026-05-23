package com.fpm_2025.reportingservice.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "budget_alerts", indexes = {
    @Index(name = "idx_budget_alerts_user", columnList = "user_id"),
    @Index(name = "idx_budget_alerts_budget", columnList = "budget_id"),
    @Index(name = "idx_budget_alerts_unread", columnList = "user_id, is_read")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetAlert {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "budget_id", nullable = false)
    private Long budgetId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;
    
    @Column(name = "threshold_percent", nullable = false)
    private Integer thresholdPercent;
    
    @Column(name = "amount_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountLimit;
    
    @Column(name = "amount_used", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountUsed;
    
    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;
    
    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", insertable = false, updatable = false)
    private Budget budget;
    
    @PrePersist
    protected void onCreate() {
        triggeredAt = LocalDateTime.now();
    }
}
