package com.fpm_2025.reportingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tổng hợp toàn bộ budgets của user trong một tháng.
 *
 * <p>Trả về khi client gọi GET /api/v1/budgets/summary:
 * <ul>
 *   <li>Tổng hạn mức (totalLimit)</li>
 *   <li>Tổng đã chi (totalUsed)</li>
 *   <li>Còn lại (totalRemaining)</li>
 *   <li>Số budget đang cảnh báo (warningCount)</li>
 *   <li>Số budget đã vượt (overBudgetCount)</li>
 *   <li>Chi tiết từng budget</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetSummaryDTO {

    private String yearMonth;

    private BigDecimal totalLimit;

    private BigDecimal totalUsed;

    private BigDecimal totalRemaining;

    /** Phần trăm tổng đã chi / tổng hạn mức */
    private BigDecimal overallUsagePercentage;

    /** Số budget WARNING (>=80%) */
    private int warningCount;

    /** Số budget OVER_BUDGET (>100%) */
    private int overBudgetCount;

    /** Tổng số budget đang active */
    private int totalBudgets;

    /** Chi tiết từng budget */
    private List<BudgetDTO> budgets;
}
