package com.fpm_2025.reportingservice.service;

import com.fpm2025.protocol.transaction.DateRangeRequest;
import com.fpm2025.protocol.transaction.TransactionGrpcServiceGrpc;
import com.fpm2025.protocol.transaction.TransactionsResponse;
import com.fpm2025.protocol.category.CategoryGrpcServiceGrpc;
import com.fpm2025.protocol.category.CategoryTypeRequest;
import com.fpm2025.protocol.category.CategoriesResponse;
import com.fpm2025.reporting.service.dto.request.DashboardRequest;
import com.fpm2025.reporting.service.dto.response.DashboardResponse;
import com.fpm2025.reporting.service.dto.response.CategorySpending;
import com.fpm2025.reporting.service.entity.BudgetEntity;
import com.fpm2025.reporting.service.entity.MonthlySummaryEntity;
import com.fpm2025.reporting.service.repository.BudgetRepository;
import com.fpm2025.reporting.service.repository.MonthlySummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    @GrpcClient("transaction-service")
    private TransactionGrpcServiceGrpc.TransactionGrpcServiceBlockingStub transactionStub;

    @GrpcClient("category-service")
    private CategoryGrpcServiceGrpc.CategoryGrpcServiceBlockingStub categoryStub;

    private final BudgetRepository budgetRepository;
    private final MonthlySummaryRepository monthlySummaryRepository;

    public DashboardResponse getDashboard(DashboardRequest request) {
        log.info("Getting dashboard for user: {}, month: {}", request.getUserId(), request.getMonth());

        // Get date range for the month
        LocalDate startDate = request.getMonth().withDayOfMonth(1);
        LocalDate endDate = request.getMonth().withDayOfMonth(request.getMonth().lengthOfMonth());

        // Fetch transactions from transaction-service via gRPC
        DateRangeRequest grpcRequest = DateRangeRequest.newBuilder()
                .setUserId(request.getUserId())
                .setStartDate(startDate.format(DateTimeFormatter.ISO_DATE))
                .setEndDate(endDate.format(DateTimeFormatter.ISO_DATE))
                .build();

        TransactionsResponse transactionsResponse = transactionStub.getTransactionsByDateRange(grpcRequest);

        // Calculate summary
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        Map<Long, BigDecimal> categorySpendingMap = new HashMap<>();

        for (var transaction : transactionsResponse.getTransactionsList()) {
            BigDecimal amount = BigDecimal.valueOf(transaction.getAmount().getAmount());
            
            if ("INCOME".equals(transaction.getType())) {
                totalIncome = totalIncome.add(amount);
            } else if ("EXPENSE".equals(transaction.getType())) {
                totalExpense = totalExpense.add(amount);
                
                // Aggregate by category
                Long categoryId = transaction.getCategoryId();
                categorySpendingMap.merge(categoryId, amount, BigDecimal::add);
            }
        }

        // Fetch categories
        CategoriesResponse categoriesResponse = categoryStub.getCategoriesByType(
                CategoryTypeRequest.newBuilder()
                        .setType("EXPENSE")
                        .setUserId(request.getUserId())
                        .build()
        );

        Map<Long, String> categoryNameMap = categoriesResponse.getCategoriesList().stream()
                .collect(Collectors.toMap(
                        category -> category.getId(),
                        category -> category.getName()
                ));

        // Build category spending list
        List<CategorySpending> categorySpendingList = categorySpendingMap.entrySet().stream()
                .map(entry -> {
                    BigDecimal percentage = totalExpense.compareTo(BigDecimal.ZERO) == 0 
                            ? BigDecimal.ZERO 
                            : entry.getValue().multiply(BigDecimal.valueOf(100))
                                    .divide(totalExpense, 2, java.math.RoundingMode.HALF_UP);
                    
                    return CategorySpending.builder()
                            .categoryId(entry.getKey())
                            .categoryName(categoryNameMap.getOrDefault(entry.getKey(), "Unknown"))
                            .amount(entry.getValue())
                            .percentage(percentage.doubleValue())
                            .build();
                })
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .collect(Collectors.toList());

        // Fetch budgets
        List<BudgetEntity> budgets = budgetRepository.findByUserIdAndMonth(
                request.getUserId(), 
                request.getMonth()
        );

        // Build response
        return DashboardResponse.builder()
                .userId(request.getUserId())
                .month(request.getMonth())
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .balance(totalIncome.subtract(totalExpense))
                .transactionCount(transactionsResponse.getTransactionsList().size())
                .categorySpending(categorySpendingList)
                .budgets(budgets.stream()
                        .map(this::toBudgetStatus)
                        .collect(Collectors.toList()))
                .build();
    }

    public DashboardResponse getQuickSummary(DashboardRequest request) {
        log.info("Getting quick summary for user: {}", request.getUserId());

        // Try to get from monthly summary cache
        Optional<MonthlySummaryEntity> summaryOpt = monthlySummaryRepository
                .findByUserIdAndMonth(request.getUserId(), request.getMonth());

        if (summaryOpt.isPresent()) {
            MonthlySummaryEntity summary = summaryOpt.get();
            return DashboardResponse.builder()
                    .userId(request.getUserId())
                    .month(request.getMonth())
                    .totalIncome(summary.getTotalIncome())
                    .totalExpense(summary.getTotalExpense())
                    .balance(summary.getBalance())
                    .transactionCount(summary.getTransactionCount())
                    .build();
        }

        // If not cached, get full dashboard
        return getDashboard(request);
    }

    private DashboardResponse.BudgetStatus toBudgetStatus(BudgetEntity budget) {
        String status = "OK";
        if (budget.getUsagePercentage() >= 100) {
            status = "EXCEEDED";
        } else if (budget.getUsagePercentage() >= 80) {
            status = "WARNING";
        }

        return DashboardResponse.BudgetStatus.builder()
                .categoryId(budget.getCategoryId())
                .budgetAmount(budget.getLimited())
                .usedAmount(budget.getUsed())
                .remainingAmount(budget.getRemaining())
                .percentage(budget.getUsagePercentage())
                .status(status)
                .build();
    }
}