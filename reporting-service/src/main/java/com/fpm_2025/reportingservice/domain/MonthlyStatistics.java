package com.fpm_2025.reportingservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyStatistics {
    private Long userId;
    private String month;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netIncome;
    private Integer transactionCount;
    private BigDecimal avgDailyExpense;
    private String topExpenseCategory;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
