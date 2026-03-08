package com.fpm_2025.reportingservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    
    private Long userId;
    private String yearMonth;
    private Summary summary;
    private List<CategoryBreakdown> categoryBreakdowns;
    private List<BudgetStatus> budgetStatuses;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private BigDecimal totalIncome;
        private BigDecimal totalExpense;
        private BigDecimal netIncome;
        private Integer transactionCount;
        private BigDecimal avgDailyExpense;
        private String topExpenseCategory;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryBreakdown {
        private Long categoryId;
        private String categoryName;
        private String type;
        private BigDecimal amount;
        private Integer transactionCount;
        private BigDecimal percentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetStatus {
        private Long categoryId;
        private String categoryName;
        private BigDecimal amountLimit;
        private BigDecimal amountUsed;
        private BigDecimal remainingAmount;
        private BigDecimal usagePercentage;
        private String status;
    }
}