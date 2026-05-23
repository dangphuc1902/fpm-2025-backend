package com.fpm_2025.reportingservice.service;

import com.fpm_2025.reportingservice.domain.*;
import com.fpm_2025.reportingservice.domain.model.Budget;
import com.fpm_2025.reportingservice.domain.model.CategorySummary;
import com.fpm_2025.reportingservice.entity.ReportEntity;
import com.fpm_2025.reportingservice.entity.TransactionSummaryEntity;
import com.fpm_2025.reportingservice.repository.BudgetRepository;
import com.fpm_2025.reportingservice.repository.CategorySummaryRepository;
import com.fpm_2025.reportingservice.repository.ReportRepository;
import com.fpm_2025.reportingservice.repository.TransactionSummaryRepository;
import com.fpm_2025.reportingservice.dto.response.BudgetComparisonItem;
import com.fpm_2025.reportingservice.dto.response.ChartDataResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fpm_2025.reportingservice.dto.request.ReportRequest;
import com.fpm_2025.reportingservice.dto.response.ReportResponse;
import com.fpm_2025.reportingservice.domain.valueobject.ExportFormat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import com.fpm_2025.reportingservice.exception.ResourceNotFoundException;

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
    private final CategorySummaryRepository categorySummaryRepository;
    private final BudgetRepository budgetRepository;
    private final com.fpm_2025.reportingservice.repository.ExportJobRepository exportJobRepository;

    /**
     * Generate monthly report for user
     * 
     * @param userId User ID
     * @param yearMonth Month to generate report (e.g., "2025-11")
     * @param format Report format (PDF, EXCEL, CSV)
     * @return Report metadata with download URL
     */
    @Transactional
    public ReportResponse generateMonthlyReport(ReportRequest request) {
        
        Long userId = request.getUserId();
        ReportFormat format = ReportFormat.valueOf(request.getFormat().name());
        
        log.info("Generating {} report for user: {}, dates: {} to {}", 
            format, userId, request.getStartDate(), request.getEndDate());

        // 1️⃣ Get transaction data for the month
        LocalDateTime startDate = request.getStartDate() != null ? 
            request.getStartDate().atStartOfDay() : 
            LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
            
        LocalDateTime endDate = request.getEndDate() != null ? 
            request.getEndDate().atTime(23, 59, 59) : 
            LocalDateTime.now().withHour(23).withMinute(59);
            
        String period = startDate.getYear() + "-" + String.format("%02d", startDate.getMonthValue());

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
            .period(period)
            .fileName(generateFileName(userId, period, format))
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

    public byte[] downloadReport(String fileUrl) {
        return reportGenerator.downloadFromStorage(fileUrl);
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

        // 1️⃣ Fetch all transactions for the 6-month period in a single call
        YearMonth startYm = YearMonth.from(now.minusMonths(5));
        LocalDateTime startDateRange = startYm.atDay(1).atStartOfDay();
        LocalDateTime endDateRange = YearMonth.from(now).atEndOfMonth().atTime(23, 59, 59);

        List<TransactionData> allTransactions = transactionClient
            .getTransactionsByDateRange(userId, startDateRange, endDateRange);

        // 2️⃣ Group transactions by YearMonth
        Map<YearMonth, List<TransactionData>> transactionsByMonth = allTransactions.stream()
            .collect(Collectors.groupingBy(t -> YearMonth.from(t.getTransactionDate())));

        List<TrendData> trend = new java.util.ArrayList<>();

        // 3️⃣ Calculate income/expense for each month
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.from(now.minusMonths(i));
            List<TransactionData> transactions = transactionsByMonth
                .getOrDefault(ym, new java.util.ArrayList<>());

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
            .format(ExportFormat.valueOf(entity.getType()))
            .period(entity.getPeriod())
            .fileName(entity.getFileName())
            .fileUrl(entity.getFileUrl())
            .fileSize(entity.getFileSize())
            .status(entity.getStatus().name())
            .createdAt(entity.getCreatedAt())
            .build();
    }

    // =========================================================================
    // GET /api/v1/reports/spending-by-category
    // =========================================================================

    /**
     * Aggregate chi tiêu theo danh mục cho 1 tháng.
     * Ưu tiên dữ liệu từ CategorySummary (DB caching từ Kafka event).
     * Fallback: gọi gRPC sang transaction-service nếu chưa có data.
     */
    @Cacheable(value = "spending-by-category", key = "#userId + '-' + #yearMonth + '-' + #type")
    public ChartDataResponse getSpendingByCategory(Long userId, String yearMonth, String type) {
        log.info("getSpendingByCategory: userId={}, month={}, type={}", userId, yearMonth, type);

        CategorySummary.CategorySummaryType summaryType;
        try {
            summaryType = CategorySummary.CategorySummaryType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            summaryType = CategorySummary.CategorySummaryType.EXPENSE;
        }

        // Bước 1: Lấy từ DB (Kafka consumer đã aggregate sẵn)
        List<CategorySummary> dbSummaries = categorySummaryRepository
                .findByUserIdAndYearMonthAndType(userId, yearMonth, summaryType);

        List<String> labels;
        List<BigDecimal> amounts;

        if (!dbSummaries.isEmpty()) {
            // Dùng data từ DB
            labels  = dbSummaries.stream().map(CategorySummary::getCategoryName).collect(Collectors.toList());
            amounts = dbSummaries.stream().map(CategorySummary::getTotalAmount).collect(Collectors.toList());
        } else {
            // Fallback: tính toán từ gRPC
            log.info("No DB summary found, falling back to gRPC for userId={}", userId);
            YearMonth ym = YearMonth.parse(yearMonth);
            LocalDateTime start = ym.atDay(1).atStartOfDay();
            LocalDateTime end   = ym.atEndOfMonth().atTime(23, 59, 59);

            List<TransactionData> txns = transactionClient.getTransactionsByDateRange(userId, start, end);
            final String filterType = type.toUpperCase();

            Map<String, BigDecimal> grouped = txns.stream()
                    .filter(t -> filterType.equals(t.getType()))
                    .collect(Collectors.groupingBy(
                            TransactionData::getCategoryName,
                            Collectors.reducing(BigDecimal.ZERO, TransactionData::getAmount, BigDecimal::add)));

            labels  = new ArrayList<>(grouped.keySet());
            amounts = new ArrayList<>(grouped.values());
        }

        // Build Pie Chart colors
        List<String> PALETTE = List.of(
                "#FF6B6B","#4ECDC4","#45B7D1","#96CEB4","#FFEAA7",
                "#DDA0DD","#FF9F43","#A29BFE","#6C757D","#20C997");
        List<String> colors = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) colors.add(PALETTE.get(i % PALETTE.size()));

        List<Object> data = new ArrayList<>(amounts);

        return ChartDataResponse.builder()
                .chartType("PIE")
                .labels(labels)
                .datasets(List.of(ChartDataResponse.Dataset.builder()
                        .label(type + " by Category")
                        .data(data)
                        .backgroundColor(colors)
                        .borderColor(colors)
                        .borderWidth(2)
                        .build()))
                .build();
    }

    // =========================================================================
    // GET /api/v1/reports/trends
    // =========================================================================

    /**
     * So sánh income vs expense theo từng tháng trong N tháng gần nhất.
     * Return dạng Line Chart.
     */
    @Cacheable(value = "monthly-trends", key = "#userId + '-' + #months")
    public ChartDataResponse getMonthlyTrends(Long userId, int months) {
        log.info("getMonthlyTrends: userId={}, months={}", userId, months);
        if (months <= 0 || months > 24) months = 6;

        LocalDate now = LocalDate.now();
        YearMonth startYm = YearMonth.from(now.minusMonths(months - 1));
        YearMonth endYm   = YearMonth.from(now);

        // Bước 1: Lấy từ MonthlySummary DB (Kafka consumer cập nhật)
        String startMonth = startYm.toString();
        String endMonth   = endYm.toString();

        Map<String, BigDecimal> incomeMap  = new LinkedHashMap<>();
        Map<String, BigDecimal> expenseMap = new LinkedHashMap<>();

        // Khởi tạo tất cả tháng với 0
        List<String> monthLabels = new ArrayList<>();
        for (int i = months - 1; i >= 0; i--) {
            String m = YearMonth.from(now.minusMonths(i)).toString();
            monthLabels.add(m);
            incomeMap.put(m, BigDecimal.ZERO);
            expenseMap.put(m, BigDecimal.ZERO);
        }

        // Fallback: gọi gRPC nếu chưa có summary trong DB
        LocalDateTime gRpcStart = startYm.atDay(1).atStartOfDay();
        LocalDateTime gRpcEnd   = endYm.atEndOfMonth().atTime(23, 59, 59);
        List<TransactionData> allTxns = transactionClient.getTransactionsByDateRange(userId, gRpcStart, gRpcEnd);

        for (TransactionData txn : allTxns) {
            if (txn.getTransactionDate() == null) continue;
            String m = YearMonth.from(txn.getTransactionDate()).toString();
            if (!incomeMap.containsKey(m)) continue;
            if ("INCOME".equals(txn.getType())) {
                incomeMap.merge(m, txn.getAmount(), BigDecimal::add);
            } else {
                expenseMap.merge(m, txn.getAmount(), BigDecimal::add);
            }
        }

        List<Object> incomeData  = new ArrayList<>(incomeMap.values());
        List<Object> expenseData = new ArrayList<>(expenseMap.values());

        return ChartDataResponse.builder()
                .chartType("LINE")
                .labels(monthLabels)
                .datasets(List.of(
                        ChartDataResponse.Dataset.builder()
                                .label("Thu nhập")
                                .data(incomeData)
                                .backgroundColor(List.of("rgba(40,167,69,0.2)"))
                                .borderColor(List.of("#28A745"))
                                .borderWidth(2)
                                .build(),
                        ChartDataResponse.Dataset.builder()
                                .label("Chi tiêu")
                                .data(expenseData)
                                .backgroundColor(List.of("rgba(255,107,107,0.2)"))
                                .borderColor(List.of("#FF6B6B"))
                                .borderWidth(2)
                                .build()))
                .build();
    }

    // =========================================================================
    // GET /api/v1/reports/budget-comparison
    // =========================================================================

    /**
     * So sánh ngân sách đặt ra vs chi tiêu thực tế theo danh mục.
     * Kết hợp dữ liệu từ Budget entity và CategorySummary.
     */
    public List<BudgetComparisonItem> getBudgetComparison(Long userId, String yearMonth) {
        log.info("getBudgetComparison: userId={}, month={}", userId, yearMonth);

        // Lấy toàn bộ budget active trong tháng
        List<Budget> budgets = budgetRepository.findActiveBudgetsByYearMonth(userId, yearMonth);

        // Lấy CategorySummary để biết chi tiêu thực tế
        List<CategorySummary> summaries = categorySummaryRepository
                .findByUserIdAndYearMonth(userId, yearMonth);

        // Index theo categoryId
        Map<Long, BigDecimal> actualByCategory = summaries.stream()
                .filter(cs -> CategorySummary.CategorySummaryType.EXPENSE.equals(cs.getType()))
                .collect(Collectors.toMap(
                        CategorySummary::getCategoryId,
                        CategorySummary::getTotalAmount,
                        BigDecimal::add));

        List<BudgetComparisonItem> result = new ArrayList<>();

        // Danh mục có budget
        Set<Long> budgetedCategories = new HashSet<>();
        for (Budget b : budgets) {
            BigDecimal actual = actualByCategory.getOrDefault(b.getCategoryId(), BigDecimal.ZERO);
            BigDecimal diff   = b.getAmountLimit().subtract(actual);
            double pct = b.getAmountLimit().compareTo(BigDecimal.ZERO) == 0
                    ? 0.0
                    : actual.multiply(BigDecimal.valueOf(100))
                            .divide(b.getAmountLimit(), 2, RoundingMode.HALF_UP)
                            .doubleValue();

            String status = pct >= 100 ? "OVER_BUDGET"
                    : pct >= 95 ? "CRITICAL"
                    : pct >= 80 ? "WARNING" : "ON_TRACK";

            result.add(BudgetComparisonItem.builder()
                    .categoryId(b.getCategoryId())
                    .categoryName(b.getCategoryName())
                    .budgetLimit(b.getAmountLimit())
                    .actualSpending(actual)
                    .difference(diff)
                    .usagePercent(pct)
                    .status(status)
                    .build());

            budgetedCategories.add(b.getCategoryId());
        }

        // Danh mục có chi tiêu nhưng không có budget
        for (CategorySummary cs : summaries) {
            if (CategorySummary.CategorySummaryType.EXPENSE.equals(cs.getType())
                    && !budgetedCategories.contains(cs.getCategoryId())) {
                result.add(BudgetComparisonItem.builder()
                        .categoryId(cs.getCategoryId())
                        .categoryName(cs.getCategoryName())
                        .budgetLimit(BigDecimal.ZERO)
                        .actualSpending(cs.getTotalAmount())
                        .difference(cs.getTotalAmount().negate())
                        .usagePercent(null)
                        .status("NO_BUDGET")
                        .build());
            }
        }

        // Sắp xếp theo thiếu ngân sách nhất (over-budget lên trước)
        result.sort(Comparator.comparing(BudgetComparisonItem::getDifference));
        return result;
    }

    /**
     * Submit an asynchronous export job
     */
    @org.springframework.transaction.annotation.Transactional
    public Long submitExportJob(Long userId, com.fpm_2025.reportingservice.dto.request.ReportRequest request) {
        log.info("Submitting async export job for user: {}, format: {}", 
            userId, request.getFormat());
        
        com.fpm_2025.reportingservice.domain.model.ExportJob job = com.fpm_2025.reportingservice.domain.model.ExportJob.builder()
            .userId(userId)
            .format(request.getFormat())
            .period(request.getStartDate().getYear() + "-" + String.format("%02d", request.getStartDate().getMonthValue()))
            .status(com.fpm_2025.reportingservice.domain.valueobject.ExportStatus.PENDING)
            .build();
            
        com.fpm_2025.reportingservice.domain.model.ExportJob savedJob = exportJobRepository.save(job);
        
        // Trigger async processing
        processExportJobAsync(savedJob.getId(), request);
        
        return savedJob.getId();
    }

    /**
     * Process export job asynchronously
     */
    @org.springframework.scheduling.annotation.Async
    @org.springframework.transaction.annotation.Transactional
    public void processExportJobAsync(Long jobId, com.fpm_2025.reportingservice.dto.request.ReportRequest request) {
        log.info("Processing export job {} asynchronously", jobId);
        
        com.fpm_2025.reportingservice.domain.model.ExportJob job = exportJobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
            
        try {
            job.markAsProcessing();
            exportJobRepository.save(job);

            // Reuse logic from generateMonthlyReport
            Long userId = request.getUserId();
            com.fpm_2025.reportingservice.domain.valueobject.ExportFormat format = request.getFormat();
            java.time.LocalDateTime startDate = request.getStartDate().atStartOfDay();
            java.time.LocalDateTime endDate = request.getEndDate().atTime(23, 59, 59);

            List<com.fpm_2025.reportingservice.domain.TransactionData> transactions = transactionClient
                .getTransactionsByDateRange(userId, startDate, endDate);
            List<com.fpm_2025.reportingservice.domain.WalletData> wallets = walletClient.getUserWallets(userId);
            com.fpm_2025.reportingservice.domain.MonthlyStatistics stats = calculateMonthlyStatistics(transactions, wallets, startDate, endDate);

            byte[] reportData = reportGenerator.generate(transactions, stats, wallets, 
                com.fpm_2025.reportingservice.domain.ReportFormat.valueOf(format.name()));

            String fileName = generateFileName(userId, job.getPeriod(), 
                com.fpm_2025.reportingservice.domain.ReportFormat.valueOf(format.name()));
            String fileUrl = reportGenerator.uploadToStorage(reportData, fileName);

            job.markAsDone(fileName, fileUrl, (long) reportData.length);
            exportJobRepository.save(job);
            
            log.info("Export job {} completed successfully", jobId);
            
        } catch (Exception e) {
            log.error("Export job {} failed", jobId, e);
            job.markAsFailed(e.getMessage());
            exportJobRepository.save(job);
        }
    }

    public com.fpm_2025.reportingservice.domain.model.ExportJob getExportJobStatus(Long jobId, Long userId) {
        com.fpm_2025.reportingservice.domain.model.ExportJob job = exportJobRepository.findById(jobId)
            .orElseThrow(() -> new com.fpm_2025.reportingservice.exception.ResourceNotFoundException("Job not found: " + jobId));
            
        if (!job.getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized access to export job");
        }
        
        return job;
    }
}