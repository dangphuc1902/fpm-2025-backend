package com.fpm_2025.reportingservice.service;

import com.fpm_2025.grpc.TransactionProto;
import com.fpm_2025.reporting.domain.model.CategorySummary;
import com.fpm_2025.reporting.domain.model.MonthlySummary;
import com.fpm_2025.reporting.grpc.client.TransactionGrpcClient;
import com.fpm_2025.reporting.repository.CategorySummaryRepository;
import com.fpm_2025.reporting.repository.MonthlySummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {
    
    private final TransactionGrpcClient transactionClient;
    private final MonthlySummaryRepository monthlySummaryRepository;
    private final CategorySummaryRepository categorySummaryRepository;
    
    @Cacheable(value = "statistics", key = "#userId + '-' + #startDate + '-' + #endDate")
    public Map<String, Object> getComprehensiveStatistics(
        Long userId, LocalDate startDate, LocalDate endDate
    ) {
        log.info("Generating comprehensive statistics for user {} from {} to {}", 
            userId, startDate, endDate);
        
        Map<String, Object> stats = new HashMap<>();
        
        // Basic statistics
        stats.put("basicStats", getBasicStatistics(userId, startDate, endDate));
        
        // Trend analysis
        stats.put("trends", getTrendAnalysis(userId, startDate, endDate));
        
        // Category insights
        stats.put("categoryInsights", getCategoryInsights(userId, startDate, endDate));
        
        // Time-based patterns
        stats.put("patterns", getSpendingPatterns(userId, startDate, endDate));
        
        // Comparison with previous period
        stats.put("periodComparison", getPeriodComparison(userId, startDate, endDate));
        
        // Financial health score
        stats.put("healthScore", calculateFinancialHealthScore(userId, startDate, endDate));
        
        return stats;
    }
    
    private Map<String, Object> getBasicStatistics(Long userId, LocalDate startDate, LocalDate endDate) {
        List<TransactionProto> transactions = transactionClient.getTransactionsByDateRange(
            userId, startDate, endDate, null
        );
        
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        int incomeCount = 0;
        int expenseCount = 0;
        
        for (TransactionProto tx : transactions) {
            BigDecimal amount = new BigDecimal(tx.getAmount());
            if ("INCOME".equals(tx.getType())) {
                totalIncome = totalIncome.add(amount);
                incomeCount++;
            } else if ("EXPENSE".equals(tx.getType())) {
                totalExpense = totalExpense.add(amount);
                expenseCount++;
            }
        }
        
        BigDecimal avgIncome = incomeCount > 0 
            ? totalIncome.divide(BigDecimal.valueOf(incomeCount), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        
        BigDecimal avgExpense = expenseCount > 0
            ? totalExpense.divide(BigDecimal.valueOf(expenseCount), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        
        Map<String, Object> basicStats = new HashMap<>();
        basicStats.put("totalIncome", totalIncome);
        basicStats.put("totalExpense", totalExpense);
        basicStats.put("netSavings", totalIncome.subtract(totalExpense));
        basicStats.put("savingsRate", calculateSavingsRate(totalIncome, totalExpense));
        basicStats.put("totalTransactions", transactions.size());
        basicStats.put("incomeTransactions", incomeCount);
        basicStats.put("expenseTransactions", expenseCount);
        basicStats.put("averageIncome", avgIncome);
        basicStats.put("averageExpense", avgExpense);
        
        return basicStats;
    }
    
    private Map<String, Object> getTrendAnalysis(Long userId, LocalDate startDate, LocalDate endDate) {
        // Get monthly summaries for trend analysis
        List<MonthlySummary> summaries = monthlySummaryRepository
            .findByUserIdAndDateRangeAndWallet(userId, startDate, endDate, null);
        
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
                "month", s.getMonthStart(),
                "income", s.getTotalIncome(),
                "expense", s.getTotalExpense(),
                "savings", s.getNetSavings()
            ))
            .collect(Collectors.toList()));
        
        return trends;
    }
    
    private Map<String, Object> getCategoryInsights(Long userId, LocalDate startDate, LocalDate endDate) {
        List<CategorySummary> summaries = categorySummaryRepository
            .findByUserIdAndDateRange(userId, startDate, endDate);
        
        // Find top spending categories
        List<Map<String, Object>> topCategories = summaries.stream()
            .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
            .limit(5)
            .map(cs -> Map.of(
                "categoryId", cs.getCategoryId(),
                "categoryName", cs.getCategoryName(),
                "totalAmount", cs.getTotalAmount(),
                "transactionCount", cs.getTransactionCount(),
                "averageAmount", cs.getAvgAmount()
            ))
            .collect(Collectors.toList());
        
        // Calculate category concentration (Herfindahl index)
        BigDecimal totalExpense = summaries.stream()
            .map(CategorySummary::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal concentration = summaries.stream()
            .map(cs -> {
                BigDecimal share = cs.getTotalAmount().divide(totalExpense, 4, RoundingMode.HALF_UP);
                return share.multiply(share);
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Map<String, Object> insights = new HashMap<>();
        insights.put("topCategories", topCategories);
        insights.put("categoryCount", summaries.size());
        insights.put("concentration", concentration);
        insights.put("diversificationScore", BigDecimal.ONE.subtract(concentration).multiply(BigDecimal.valueOf(100)));
        
        return insights;
    }
    
    private Map<String, Object> getSpendingPatterns(Long userId, LocalDate startDate, LocalDate endDate) {
        List<TransactionProto> transactions = transactionClient.getTransactionsByDateRange(
            userId, startDate, endDate, null
        );
        
        // Group by day of week
        Map<String, BigDecimal> byDayOfWeek = transactions.stream()
            .filter(tx -> "EXPENSE".equals(tx.getType()))
            .collect(Collectors.groupingBy(
                tx -> LocalDate.parse(tx.getTransactionDate()).getDayOfWeek().toString(),
                Collectors.reducing(BigDecimal.ZERO, 
                    tx -> new BigDecimal(tx.getAmount()), 
                    BigDecimal::add)
            ));
        
        // Group by time of day (if timestamp available)
        Map<String, Integer> byTimeOfDay = new HashMap<>();
        byTimeOfDay.put("MORNING", 0);
        byTimeOfDay.put("AFTERNOON", 0);
        byTimeOfDay.put("EVENING", 0);
        byTimeOfDay.put("NIGHT", 0);
        
        // Find largest transaction
        Optional<TransactionProto> largestTx = transactions.stream()
            .filter(tx -> "EXPENSE".equals(tx.getType()))
            .max(Comparator.comparing(tx -> new BigDecimal(tx.getAmount())));
        
        Map<String, Object> patterns = new HashMap<>();
        patterns.put("spendingByDayOfWeek", byDayOfWeek);
        patterns.put("spendingByTimeOfDay", byTimeOfDay);
        
        if (largestTx.isPresent()) {
            patterns.put("largestTransaction", Map.of(
                "amount", largestTx.get().getAmount(),
                "date", largestTx.get().getTransactionDate(),
                "description", largestTx.get().getDescription()
            ));
        }
        
        return patterns;
    }
    
    private Map<String, Object> getPeriodComparison(Long userId, LocalDate startDate, LocalDate endDate) {
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        LocalDate previousStart = startDate.minusDays(daysBetween + 1);
        LocalDate previousEnd = startDate.minusDays(1);
        
        Map<String, Object> currentPeriod = getBasicStatistics(userId, startDate, endDate);
        Map<String, Object> previousPeriod = getBasicStatistics(userId, previousStart, previousEnd);
        
        BigDecimal currentIncome = (BigDecimal) currentPeriod.get("totalIncome");
        BigDecimal previousIncome = (BigDecimal) previousPeriod.get("totalIncome");
        BigDecimal currentExpense = (BigDecimal) currentPeriod.get("totalExpense");
        BigDecimal previousExpense = (BigDecimal) previousPeriod.get("totalExpense");
        
        Map<String, Object> comparison = new HashMap<>();
        comparison.put("currentPeriod", currentPeriod);
        comparison.put("previousPeriod", previousPeriod);
        comparison.put("incomeChange", calculateChangePercentage(previousIncome, currentIncome));
        comparison.put("expenseChange", calculateChangePercentage(previousExpense, currentExpense));
        
        return comparison;
    }
    
    private Map<String, Object> calculateFinancialHealthScore(Long userId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> basicStats = getBasicStatistics(userId, startDate, endDate);
        
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
    
    private BigDecimal calculateChangePercentage(BigDecimal oldValue, BigDecimal newValue) {
        if (oldValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
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