package com.fpm_2025.reportingservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "budgets", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "category_id", "month"})
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "month", nullable = false)
    private LocalDate month;

    @Column(name = "limited", nullable = false, precision = 15, scale = 2)
    private BigDecimal limited;

    @Column(name = "used", nullable = false, precision = 15, scale = 2)
    private BigDecimal used = BigDecimal.ZERO;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Transient
    public BigDecimal getRemaining() {
        return limited.subtract(used);
    }

    @Transient
    public double getUsagePercentage() {
        if (limited.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        return used.divide(limited, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    @Transient
    public boolean isExceeded() {
        return used.compareTo(limited) > 0;
    }
}