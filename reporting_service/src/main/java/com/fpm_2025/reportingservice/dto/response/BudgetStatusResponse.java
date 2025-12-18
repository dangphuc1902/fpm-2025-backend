package com.fpm_2025.reportingservice.dto.response;

import com.fpm_2025.reportingservice.domain.valueobject.BudgetPeriod;
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
public class BudgetStatusResponse {
    
    private Long budgetId;
    private String name;
    private String categoryName;
    private BigDecimal amountLimit;
    private BigDecimal currentSpent;
    private BigDecimal remainingAmount;
    private BigDecimal usagePercentage;
    private BudgetPeriod period;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status; // ON_TRACK, WARNING, CRITICAL, OVER_BUDGET
    private Boolean isActive;
    private Integer daysRemaining;
    private BigDecimal dailyAverageSpent;
    private BigDecimal projectedTotal;
    private List<CategoryBreakdown> categoryBreakdowns;
    private List<Alert> recentAlerts;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryBreakdown {
        private Long categoryId;
        private String categoryName;
        private BigDecimal budgetAmount;
        private BigDecimal spentAmount;
        private BigDecimal percentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Alert {
        private String type;
        private String message;
        private LocalDate date;
        private Boolean isRead;
    }
}