package com.fpm_2025.reportingservice.service;

import com.fpm_2025.reportingservice.domain.model.Budget;
import com.fpm_2025.reportingservice.domain.model.CategorySummary;
import com.fpm_2025.reportingservice.domain.model.MonthlySummary;
import com.fpm_2025.reportingservice.dto.request.DashboardRequest;
import com.fpm_2025.reportingservice.dto.response.DashboardResponse;
import com.fpm_2025.reportingservice.repository.BudgetRepository;
import com.fpm_2025.reportingservice.repository.CategorySummaryRepository;
import com.fpm_2025.reportingservice.repository.MonthlySummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MonthlySummaryRepository monthlySummaryRepository;
    private final CategorySummaryRepository categorySummaryRepository;
    private final BudgetRepository budgetRepository;

    @org.springframework.cache.annotation.Cacheable(value = "dashboard")
    public DashboardResponse getDashboard(DashboardRequest request) {
        log.info("Getting dashboard for user: {}, month: {}", request.getUserId(), request.getYearMonth());

        String yearMonth = request.getYearMonth();

        // Get monthly summary
        Optional<MonthlySummary> summaryOpt = monthlySummaryRepository
                .findByUserIdAndYearMonth(request.getUserId(), yearMonth);

        // Build summary
        DashboardResponse.Summary summary;
        if (summaryOpt.isPresent()) {
            MonthlySummary ms = summaryOpt.get();
            summary = DashboardResponse.Summary.builder()
                    .totalIncome(ms.getTotalIncome())
                    .totalExpense(ms.getTotalExpense())
                    .netIncome(ms.getNetIncome())
                    .transactionCount(ms.getTransactionCount())
                    .avgDailyExpense(ms.getAvgDailyExpense())
                    .topExpenseCategory(ms.getTopExpenseCategory())
                    .build();
        } else {
            summary = DashboardResponse.Summary.builder().build();
        }

        // Get category breakdown
        List<CategorySummary> categorySummaries = categorySummaryRepository
                .findByUserIdAndYearMonth(request.getUserId(), yearMonth);

        List<DashboardResponse.CategoryBreakdown> categoryBreakdowns = categorySummaries.stream()
                .map(cs -> DashboardResponse.CategoryBreakdown.builder()
                        .categoryId(cs.getCategoryId())
                        .categoryName(cs.getCategoryName())
                        .type(cs.getType().name())
                        .amount(cs.getTotalAmount())
                        .transactionCount(cs.getTransactionCount())
                        .percentage(cs.getPercentage())
                        .build())
                .collect(Collectors.toList());

        // Get budget statuses
        List<Budget> budgets = budgetRepository
                .findActiveBudgetsByYearMonth(request.getUserId(), yearMonth);

        List<DashboardResponse.BudgetStatus> budgetStatuses = budgets.stream()
                .map(this::toBudgetStatus)
                .collect(Collectors.toList());

        return DashboardResponse.builder()
                .userId(request.getUserId())
                .yearMonth(yearMonth)
                .summary(summary)
                .categoryBreakdowns(categoryBreakdowns)
                .budgetStatuses(budgetStatuses)
                .build();
    }

    private DashboardResponse.BudgetStatus toBudgetStatus(Budget budget) {
        String status = "OK";
        if (budget.isOverBudget()) {
            status = "EXCEEDED";
        } else if (budget.getUsagePercentage().intValue() >= 80) {
            status = "WARNING";
        }

        return DashboardResponse.BudgetStatus.builder()
                .categoryId(budget.getCategoryId())
                .categoryName(budget.getCategoryName())
                .amountLimit(budget.getAmountLimit())
                .amountUsed(budget.getAmountUsed())
                .remainingAmount(budget.getRemainingAmount())
                .usagePercentage(budget.getUsagePercentage())
                .status(status)
                .build();
    }
}