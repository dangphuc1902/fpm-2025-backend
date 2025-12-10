package com.fpm_2025.reportingservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "monthly_summaries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySummaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "month", nullable = false)
    private LocalDate month;

    @Column(name = "total_income", precision = 15, scale = 2)
    private BigDecimal totalIncome = BigDecimal.ZERO;

    @Column(name = "total_expense", precision = 15, scale = 2)
    private BigDecimal totalExpense = BigDecimal.ZERO;

    @Column(name = "transaction_count")
    private Integer transactionCount = 0;

    @Transient
    public BigDecimal getBalance() {
        return totalIncome.subtract(totalExpense);
    }
}