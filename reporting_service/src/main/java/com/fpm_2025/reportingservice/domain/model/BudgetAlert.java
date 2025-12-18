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
    @Index(name = "idx_budget_user", columnList = "budget_id,user_id"),
    @Index(name = "idx_sent_at", columnList = "sent_at")
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
    
    @Column(name = "alert_type", nullable = false)
    private String alertType; // THRESHOLD_REACHED, OVER_BUDGET, APPROACHING_LIMIT
    
    @Column(nullable = false, length = 200)
    private String message;
    
    @Column(name = "percentage_used", precision = 5, scale = 2)
    private BigDecimal percentageUsed;
    
    @Column(name = "amount_over", precision = 19, scale = 2)
    private BigDecimal amountOver;
    
    @Column(name = "is_read")
    private Boolean isRead = false;
    
    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;
    
    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }
}
