package com.fpm_2025.reportingservice.dto.response;

import com.fpm_2025.reportingservice.domain.valueobject.BudgetPeriod;
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
public class BudgetStatusResponse {
    
    private Long budgetId;
    private Long categoryId;
    private String categoryName;
    private BigDecimal amountLimit;
    private BigDecimal amountUsed;
    private BigDecimal remainingAmount;
    private BigDecimal usagePercentage;
    private BudgetPeriod period;
    private String yearMonth;
    private String status; // ON_TRACK, WARNING, CRITICAL, OVER_BUDGET, INACTIVE
    private Boolean isActive;
    private List<Alert> recentAlerts;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Alert {
        private Integer thresholdPercent;
        private String categoryName;
        private BigDecimal amountLimit;
        private BigDecimal amountUsed;
        private Boolean isRead;
    }
}