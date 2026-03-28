package com.fpm_2025.reportingservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO cho API budget-comparison.
 * Thể hiện ngân sách đặt ra vs chi tiêu thực tế theo từng danh mục.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetComparisonItem {
    private Long categoryId;
    private String categoryName;
    private BigDecimal budgetLimit;     // Ngân sách đặt ra (0 nếu không đặt)
    private BigDecimal actualSpending;  // Thực tế đã chi
    private BigDecimal difference;      // budgetLimit - actualSpending (âm = vượt ngân sách)
    private Double usagePercent;        // actualSpending / budgetLimit * 100
    private String status;              // ON_TRACK | WARNING | OVER_BUDGET | NO_BUDGET
}
