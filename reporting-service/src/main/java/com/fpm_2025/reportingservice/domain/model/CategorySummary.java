package com.fpm_2025.reportingservice.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "category_summaries", indexes = {
    @Index(name = "idx_cat_summary_user_month", columnList = "user_id, year_month")
},
uniqueConstraints = {
    @UniqueConstraint(name = "uk_cat_summary", columnNames = {"user_id", "year_month", "category_id"})
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
    
    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;
    
    @Column(name = "category_id", nullable = false)
    private Long categoryId;
    
    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private CategorySummaryType type;
    
    @Builder.Default
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "transaction_count", nullable = false)
    private Integer transactionCount = 0;
    
    @Column(name = "percentage", precision = 5, scale = 2)
    private BigDecimal percentage;
    
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
    
    /**
     * Enum for category summary type (EXPENSE/INCOME)
     */
    public enum CategorySummaryType {
        EXPENSE,
        INCOME
    }
}