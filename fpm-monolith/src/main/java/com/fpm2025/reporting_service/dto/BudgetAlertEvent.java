package com.fpm2025.reporting_service.dto;

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
public class BudgetAlertEvent {
    private Long userId;
    private Long budgetId;
    private String categoryName;
    private Integer thresholdPercent;
    private BigDecimal amountLimit;
    private BigDecimal amountUsed;
    private LocalDateTime triggeredAt;
}

