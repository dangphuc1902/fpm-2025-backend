package com.fpm_2025.reportingservice.domain.valueobject;

import lombok.Value;

import java.math.BigDecimal;

@Value
public class AlertThreshold {
    BigDecimal warningLevel;    // e.g., 80%
    BigDecimal criticalLevel;   // e.g., 95%
    BigDecimal overBudgetLevel; // e.g., 100%
    
    public static AlertThreshold defaultThreshold() {
        return new AlertThreshold(
            new BigDecimal("80"),
            new BigDecimal("95"),
            new BigDecimal("100")
        );
    }
    
    public String getAlertLevel(BigDecimal usagePercentage) {
        if (usagePercentage.compareTo(overBudgetLevel) >= 0) {
            return "OVER_BUDGET";
        } else if (usagePercentage.compareTo(criticalLevel) >= 0) {
            return "CRITICAL";
        } else if (usagePercentage.compareTo(warningLevel) >= 0) {
            return "WARNING";
        }
        return "NORMAL";
    }
}