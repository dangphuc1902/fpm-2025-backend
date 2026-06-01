package com.fpm_2025.reportingservice.service;

import com.fpm_2025.reportingservice.domain.model.CategorySummary;
import com.fpm_2025.reportingservice.domain.model.MonthlySummary;
import com.fpm_2025.reportingservice.repository.CategorySummaryRepository;
import com.fpm_2025.reportingservice.repository.MonthlySummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {
    
    private final MonthlySummaryRepository monthlySummaryRepository;
    private final CategorySummaryRepository categorySummaryRepository;
    
    @Cacheable(value = "statistics", key = "#userId + '-' + #startDate + '-' + #endDate")
    public Map<String, Object> getComprehensiveStatistics(
        Long userId, LocalDate startDate, LocalDate endDate
    ) {
        log.info("Generating comprehensive statistics for user {} from {} to {}", 
            userId, startDate, endDate);
        
        String startMonth = YearMonth.from(startDate).toString();
        String endMonth = YearMonth.from(endDate).toString();
        
        Map<String, Object> stats = new HashMap<>();
        
        // Basic statistics from monthly summaries
        stats.put("basicStats", getBasicStatistics(userId, startMonth, endMonth));
        
        // Trend analysis
        stats.put("trends", getTrendAnalysis(userId, startMonth, endMonth));
        
        // Category insights
        stats.put("categoryInsights", getCategoryInsights(userId, startMonth, endMonth));
        
        // Financial health score
        stats.put("healthScore", calculateFinancialHealthScore(userId, startMonth, endMonth));
        
        return stats;
    }
    
    private Map<String, Object> getBasicStatistics(Long userId, String startMonth, String endMonth) {
        List<MonthlySummary> summaries = monthlySummaryRepository
            .findByUserIdAndYearMonthBetween(userId, startMonth, endMonth);
        
        BigDecimal totalIncome = summaries.stream()
            .map(MonthlySummary::getTotalIncome)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalExpense = summaries.stream()
            .map(MonthlySummary::getTotalExpense)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int totalTransactions = summaries.stream()
            .mapToInt(MonthlySummary::getTransactionCount)
            .sum();
        
        Map<String, Object> basicStats = new HashMap<>();
        basicStats.put("totalIncome", totalIncome);
        basicStats.put("totalExpense", totalExpense);
        basicStats.put("netIncome", totalIncome.subtract(totalExpense));
        basicStats.put("savingsRate", calculateSavingsRate(totalIncome, totalExpense));
        basicStats.put("totalTransactions", totalTransactions);
        
        return basicStats;
    }
    
    private Map<String, Object> getTrendAnalysis(Long userId, String startMonth, String endMonth) {
        List<MonthlySummary> summaries = monthlySummaryRepository
            .findByUserIdAndYearMonthBetween(userId, startMonth, endMonth);
        
        if (summaries.size() < 2) {
            return Map.of("message", "Insufficient data for trend analysis");
        }
        
        // Calculate growth rates
        BigDecimal incomeGrowth = calculateGrowthRate(
            summaries.get(0).getTotalIncome(),
            summaries.get(summaries.size() - 1).getTotalIncome()
        );
        
        BigDecimal expenseGrowth = calculateGrowthRate(
            summaries.get(0).getTotalExpense(),
            summaries.get(summaries.size() - 1).getTotalExpense()
        );
        
        // Determine trend direction
        String incomeTrend = determineTrend(incomeGrowth);
        String expenseTrend = determineTrend(expenseGrowth);
        
        Map<String, Object> trends = new HashMap<>();
        trends.put("incomeGrowthRate", incomeGrowth);
        trends.put("expenseGrowthRate", expenseGrowth);
        trends.put("incomeTrend", incomeTrend);
        trends.put("expenseTrend", expenseTrend);
        trends.put("monthlyData", summaries.stream()
            .map(s -> Map.of(
                "month", s.getYearMonth(),
                "income", s.getTotalIncome(),
                "expense", s.getTotalExpense(),
                "netIncome", s.getNetIncome()
            ))
            .collect(Collectors.toList()));
        
        return trends;
    }
    
    private Map<String, Object> getCategoryInsights(Long userId, String startMonth, String endMonth) {
        List<CategorySummary> summaries = categorySummaryRepository
            .findByUserIdAndYearMonthBetween(userId, startMonth, endMonth);
        
        // Find top spending categories
        List<Map<String, Object>> topCategories = summaries.stream()
            .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
            .limit(5)
            .map(cs -> {
                Map<String, Object> map = new HashMap<>();
                map.put("categoryId", cs.getCategoryId());
                map.put("categoryName", cs.getCategoryName());
                map.put("totalAmount", cs.getTotalAmount());
                map.put("transactionCount", cs.getTransactionCount());
                map.put("percentage", cs.getPercentage());
                return map;
            })
            .collect(Collectors.toList());
        
        // Calculate category concentration (Herfindahl index)
        BigDecimal totalExpense = summaries.stream()
            .map(CategorySummary::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal concentration = BigDecimal.ZERO;
        if (totalExpense.compareTo(BigDecimal.ZERO) > 0) {
            concentration = summaries.stream()
                .map(cs -> {
                    BigDecimal share = cs.getTotalAmount().divide(totalExpense, 4, RoundingMode.HALF_UP);
                    return share.multiply(share);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        
        Map<String, Object> insights = new HashMap<>();
        insights.put("topCategories", topCategories);
        insights.put("categoryCount", summaries.size());
        insights.put("concentration", concentration);
        insights.put("diversificationScore", BigDecimal.ONE.subtract(concentration).multiply(BigDecimal.valueOf(100)));
        
        return insights;
    }
    
    private Map<String, Object> calculateFinancialHealthScore(Long userId, String startMonth, String endMonth) {
        Map<String, Object> basicStats = getBasicStatistics(userId, startMonth, endMonth);
        
        BigDecimal savingsRate = (BigDecimal) basicStats.get("savingsRate");
        
        // Calculate health score (0-100)
        int score = 0;
        
        // Savings rate component (max 40 points)
        if (savingsRate.compareTo(BigDecimal.valueOf(20)) >= 0) {
            score += 40;
        } else if (savingsRate.compareTo(BigDecimal.ZERO) > 0) {
            score += savingsRate.multiply(BigDecimal.valueOf(2)).intValue();
        }
        
        // Expense consistency (max 30 points)
        score += 20; // Placeholder - would need more data
        
        // Budget adherence (max 30 points)
        score += 15; // Placeholder - would check budget compliance
        
        String rating;
        if (score >= 80) rating = "EXCELLENT";
        else if (score >= 60) rating = "GOOD";
        else if (score >= 40) rating = "FAIR";
        else rating = "NEEDS_IMPROVEMENT";
        
        Map<String, Object> healthScore = new HashMap<>();
        healthScore.put("score", score);
        healthScore.put("rating", rating);
        healthScore.put("savingsRateScore", Math.min(40, savingsRate.multiply(BigDecimal.valueOf(2)).intValue()));
        
        return healthScore;
    }
    
    private BigDecimal calculateSavingsRate(BigDecimal income, BigDecimal expense) {
        if (income.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return income.subtract(expense)
            .divide(income, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }
    
    private BigDecimal calculateGrowthRate(BigDecimal oldValue, BigDecimal newValue) {
        if (oldValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return newValue.subtract(oldValue)
            .divide(oldValue, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }
    
    private String determineTrend(BigDecimal growthRate) {
        if (growthRate.compareTo(BigDecimal.valueOf(10)) > 0) {
            return "INCREASING";
        } else if (growthRate.compareTo(BigDecimal.valueOf(-10)) < 0) {
            return "DECREASING";
        }
        return "STABLE";
    }
}