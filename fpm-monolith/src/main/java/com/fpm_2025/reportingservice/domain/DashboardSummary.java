package com.fpm_2025.reportingservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummary {
    private BigDecimal totalBalance;
    private BigDecimal currentMonthIncome;
    private BigDecimal currentMonthExpense;
    private BigDecimal currentMonthNet;
    private BigDecimal incomeChangePercent;
    private BigDecimal expenseChangePercent;
    private Integer transactionCount;
    private String topSpendingCategory;
}
