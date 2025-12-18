package com.fpm_2025.reportingservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    
    private Summary summary;
    private List<CategoryBreakdown> categoryBreakdowns;
    private List<TrendData> trends;
    private BudgetComparison budgetComparison;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private BigDecimal totalIncome;
        private BigDecimal totalExpense;
        private BigDecimal netSavings;
        private Integer totalTransactions;
        private BigDecimal savingsRate;
        private BigDecimal currentBalance;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryBreakdown {
        private Long categoryId;
        private String categoryName;
        private BigDecimal amount;
        private Integer transactionCount;
        private BigDecimal percentage;
        private String color;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendData {
        private LocalDate date;
        private BigDecimal income;
        private BigDecimal expense;
        private BigDecimal balance;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetComparison {
        private BigDecimal budgeted;
        private BigDecimal actual;
        private BigDecimal difference;
        private BigDecimal percentageUsed;
    }
}