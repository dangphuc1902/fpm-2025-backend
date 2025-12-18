package com.fpm_2025.reportingservice.service;

import com.fpm_2025.reportingservice.domain.*;
import com.fpm_2025.reportingservice.entity.ReportEntity;
import com.fpm_2025.reportingservice.entity.TransactionSummaryEntity;
import com.fpm_2025.reportingservice.repository.ReportRepository;
import com.fpm_2025.reportingservice.repository.TransactionSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reporting Service Implementation
 * 
 * Features:
 * - Generate financial reports (PDF, Excel, CSV)
 * - Calculate statistics and trends
 * - Cache reports in Redis
 * - Listen to transaction events for real-time aggregation
 * - Provide analytics dashboard data
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportingService {

    private final TransactionSummaryRepository summaryRepository;
    private final ReportRepository reportRepository;
    private final ReportGeneratorService reportGenerator;
    private final TransactionGrpcClient transactionClient;
    private final WalletGrpcClient walletClient;

    /**
     * Generate monthly report for user
     * 
     * @param userId User ID
     * @param yearMonth Month to generate report (e.g., "2025-11")
     * @param format Report format (PDF, EXCEL, CSV)
     * @return Report metadata with download URL
     */
    @Transactional
    public ReportResponse generateMonthlyReport(
            Long userId, 
            String yearMonth, 
            ReportFormat format) {
        
        log.info("Generating {} report for user: {}, month: {}", 
            format, userId, yearMonth);

        // 1️⃣ Get transaction data for the month
        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDateTime startDate = ym.atDay(1).atStartOfDay();
        LocalDateTime endDate = ym.atEndOfMonth().atTime(23, 59, 59);

        List<TransactionData> transactions = transactionClient
            .getTransactionsByDateRange(userId, startDate, endDate);

        // 2️⃣ Get wallet information
        List<WalletData> wallets = walletClient.getUserWallets(userId);

        // 3️⃣ Calculate statistics
        MonthlyStatistics stats = calculateMonthlyStatistics(
            transactions, wallets, startDate, endDate);

        // 4️⃣ Generate report file
        byte[] reportData = reportGenerator.generate(
            transactions, stats, wallets, format);

        // 5️⃣ Save report metadata
        ReportEntity report = ReportEntity.builder()
            .userId(userId)
            .type(format.name())
            .period(yearMonth)
            .fileName(generateFileName(userId, yearMonth, format))
            .fileSize((long) reportData.length)
            .status(ReportStatus.COMPLETED)
            .build();

        ReportEntity savedReport = reportRepository.save(report);

        // 6️⃣ Upload to storage (S3, MinIO, etc.)
        String fileUrl = reportGenerator.uploadToStorage(
            reportData, savedReport.getFileName());

        savedReport.setFileUrl(fileUrl);
        reportRepository.save(savedReport);

        log.info("Report generated successfully: reportId={}", 
            savedReport.getId());

        return mapToResponse(savedReport);
    }

    /**
     * Get user's report history
     */
    public List<ReportResponse> getUserReports(Long userId) {
        log.info("Fetching reports for user: {}", userId);
        
        List<ReportEntity> reports = reportRepository
            .findByUserIdOrderByCreatedAtDesc(userId);
        
        return reports.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Download report by ID
     */
    public byte[] downloadReport(Long reportId, Long userId) {
        log.info("Downloading report: {} for user: {}", reportId, userId);

        ReportEntity report = reportRepository
            .findByIdAndUserId(reportId, userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Report not found with id: " + reportId));

        return reportGenerator.downloadFromStorage(report.getFileUrl());
    }

    /**
     * Get monthly statistics (cached in Redis)
     */
    @Cacheable(value = "monthly-statistics", 
               key = "#userId + '-' + #yearMonth")
    public MonthlyStatistics getMonthlyStatistics(
            Long userId, 
            String yearMonth) {
        
        log.info("Calculating monthly statistics for user: {}, month: {}", 
            userId, yearMonth);

        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDateTime startDate = ym.atDay(1).atStartOfDay();
        LocalDateTime endDate = ym.atEndOfMonth().atTime(23, 59, 59);

        List<TransactionData> transactions = transactionClient
            .getTransactionsByDateRange(userId, startDate, endDate);

        List<WalletData> wallets = walletClient.getUserWallets(userId);

        return calculateMonthlyStatistics(
            transactions, wallets, startDate, endDate);
    }

    /**
     * Get spending breakdown by category (cached)
     */
    @Cacheable(value = "spending-breakdown", 
               key = "#userId + '-' + #yearMonth")
    public Map<String, BigDecimal> getSpendingBreakdown(
            Long userId, 
            String yearMonth) {
        
        log.info("Calculating spending breakdown for user: {}, month: {}", 
            userId, yearMonth);

        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDateTime startDate = ym.atDay(1).atStartOfDay();
        LocalDateTime endDate = ym.atEndOfMonth().atTime(23, 59, 59);

        List<TransactionData> transactions = transactionClient
            .getTransactionsByDateRange(userId, startDate, endDate);

        return transactions.stream()
            .filter(t -> "EXPENSE".equals(t.getType()))
            .collect(Collectors.groupingBy(
                TransactionData::getCategoryName,
                Collectors.reducing(
                    BigDecimal.ZERO,
                    TransactionData::getAmount,
                    BigDecimal::add
                )
            ));
    }

    /**
     * Get income vs expense trend (last 6 months)
     */
    @Cacheable(value = "income-expense-trend", key = "#userId")
    public List<TrendData> getIncomeExpenseTrend(Long userId) {
        log.info("Calculating income/expense trend for user: {}", userId);

        LocalDate now = LocalDate.now();
        List<TrendData> trend = new java.util.ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.from(now.minusMonths(i));
            LocalDateTime startDate = ym.atDay(1).atStartOfDay();
            LocalDateTime endDate = ym.atEndOfMonth().atTime(23, 59, 59);

            List<TransactionData> transactions = transactionClient
                .getTransactionsByDateRange(userId, startDate, endDate);

            BigDecimal income = transactions.stream()
                .filter(t -> "INCOME".equals(t.getType()))
                .map(TransactionData::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal expense = transactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .map(TransactionData::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            trend.add(TrendData.builder()
                .month(ym.toString())
                .income(income)
                .expense(expense)
                .net(income.subtract(expense))
                .build());
        }

        return trend;
    }

    /**
     * Get dashboard summary
     */
    @Cacheable(value = "dashboard-summary", key = "#userId")
    public DashboardSummary getDashboardSummary(Long userId) {
        log.info("Calculating dashboard summary for user: {}", userId);

        // Current month stats
        String currentMonth = YearMonth.now().toString();
        MonthlyStatistics currentStats = getMonthlyStatistics(
            userId, currentMonth);

        // Previous month stats for comparison
        String previousMonth = YearMonth.now().minusMonths(1).toString();
        MonthlyStatistics previousStats = getMonthlyStatistics(
            userId, previousMonth);

        // Get wallets total balance
        List<WalletData> wallets = walletClient.getUserWallets(userId);
        BigDecimal totalBalance = wallets.stream()
            .map(WalletData::getBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate changes
        BigDecimal incomeChange = calculatePercentageChange(
            previousStats.getTotalIncome(), 
            currentStats.getTotalIncome());

        BigDecimal expenseChange = calculatePercentageChange(
            previousStats.getTotalExpense(), 
            currentStats.getTotalExpense());

        return DashboardSummary.builder()
            .totalBalance(totalBalance)
            .currentMonthIncome(currentStats.getTotalIncome())
            .currentMonthExpense(currentStats.getTotalExpense())
            .currentMonthNet(currentStats.getNetIncome())
            .incomeChangePercent(incomeChange)
            .expenseChangePercent(expenseChange)
            .transactionCount(currentStats.getTransactionCount())
            .topSpendingCategory(currentStats.getTopExpenseCategory())
            .build();
    }

    // ==================== Private Methods ====================

    private MonthlyStatistics calculateMonthlyStatistics(
            List<TransactionData> transactions,
            List<WalletData> wallets,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        BigDecimal totalIncome = transactions.stream()
            .filter(t -> "INCOME".equals(t.getType()))
            .map(TransactionData::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = transactions.stream()
            .filter(t -> "EXPENSE".equals(t.getType()))
            .map(TransactionData::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Find top expense category
        String topExpenseCategory = transactions.stream()
            .filter(t -> "EXPENSE".equals(t.getType()))
            .collect(Collectors.groupingBy(
                TransactionData::getCategoryName,
                Collectors.reducing(
                    BigDecimal.ZERO,
                    TransactionData::getAmount,
                    BigDecimal::add
                )
            ))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("N/A");

        // Calculate daily average
        long days = java.time.temporal.ChronoUnit.DAYS.between(
            startDate.toLocalDate(), 
            endDate.toLocalDate()
        ) + 1;

        BigDecimal avgDailyExpense = totalExpense
            .divide(BigDecimal.valueOf(days), 2, java.math.RoundingMode.HALF_UP);

        return MonthlyStatistics.builder()
            .userId(transactions.isEmpty() ? null : transactions.get(0).getUserId())
            .month(YearMonth.from(startDate).toString())
            .totalIncome(totalIncome)
            .totalExpense(totalExpense)
            .netIncome(totalIncome.subtract(totalExpense))
            .transactionCount(transactions.size())
            .avgDailyExpense(avgDailyExpense)
            .topExpenseCategory(topExpenseCategory)
            .startDate(startDate)
            .endDate(endDate)
            .build();
    }

    private BigDecimal calculatePercentageChange(
            BigDecimal oldValue, 
            BigDecimal newValue) {
        
        if (oldValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return newValue.subtract(oldValue)
            .divide(oldValue, 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }

    private String generateFileName(
            Long userId, 
            String yearMonth, 
            ReportFormat format) {
        
        return String.format("report_%s_%s_%d.%s",
            userId,
            yearMonth,
            System.currentTimeMillis(),
            format.name().toLowerCase()
        );
    }

    private ReportResponse mapToResponse(ReportEntity entity) {
        return ReportResponse.builder()
            .id(entity.getId())
            .userId(entity.getUserId())
            .type(entity.getType())
            .period(entity.getPeriod())
            .fileName(entity.getFileName())
            .fileUrl(entity.getFileUrl())
            .fileSize(entity.getFileSize())
            .status(entity.getStatus())
            .createdAt(entity.getCreatedAt())
            .build();
    }
}