package com.fpm_2025.reportingservice.service;

import com.fpm_2025.reportingservice.domain.model.Budget;
import com.fpm_2025.reportingservice.domain.model.BudgetAlert;
import com.fpm_2025.reportingservice.domain.valueobject.BudgetPeriod;
import com.fpm_2025.reportingservice.dto.request.BudgetRequest;
import com.fpm_2025.reportingservice.dto.response.BudgetStatusResponse;
import com.fpm_2025.reportingservice.exception.ResourceNotFoundException;
import com.fpm_2025.reportingservice.grpc.client.CategoryGrpcClient;
import com.fpm_2025.reportingservice.repository.BudgetAlertRepository;
import com.fpm_2025.reportingservice.repository.BudgetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetService {
    
    private final BudgetRepository budgetRepository;
    private final BudgetAlertRepository budgetAlertRepository;
    private final CategoryGrpcClient categoryClient;
    
    @Transactional
    public Budget createBudget(Long userId, BudgetRequest request) {
        log.info("Creating budget for user {}: {}", userId, request.getName());
        
        LocalDate endDate = request.getEndDate();
        if (endDate == null && request.getPeriod() != BudgetPeriod.CUSTOM) {
            endDate = request.getPeriod().calculateEndDate(request.getStartDate());
        }
        
        Budget budget = Budget.builder()
            .userId(userId)
            .name(request.getName())
            .description(request.getDescription())
            .categoryId(request.getCategoryId())
            .walletId(request.getWalletId())
            .amountLimit(request.getAmountLimit())
            .period(request.getPeriod())
            .startDate(request.getStartDate())
            .endDate(endDate)
            .alertThreshold(request.getAlertThreshold() != null 
                ? request.getAlertThreshold() 
                : BigDecimal.valueOf(80))
            .rolloverEnabled(request.getRolloverEnabled() != null 
                ? request.getRolloverEnabled() 
                : false)
            .isActive(true)
            .currentSpent(BigDecimal.ZERO)
            .build();
        
        return budgetRepository.save(budget);
    }
    
    @Transactional
    public Budget updateBudget(Long userId, Long budgetId, BudgetRequest request) {
        Budget budget = budgetRepository.findById(budgetId)
            .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        
        if (!budget.getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized access to budget");
        }
        
        budget.setName(request.getName());
        budget.setDescription(request.getDescription());
        budget.setAmountLimit(request.getAmountLimit());
        
        if (request.getAlertThreshold() != null) {
            budget.setAlertThreshold(request.getAlertThreshold());
        }
        
        return budgetRepository.save(budget);
    }
    
    @Transactional
    public void deleteBudget(Long userId, Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
            .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        
        if (!budget.getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized access to budget");
        }
        
        budget.setIsActive(false);
        budgetRepository.save(budget);
    }
    
    @Transactional
    public void updateBudgetSpending(Long budgetId, BigDecimal amount) {
        Budget budget = budgetRepository.findById(budgetId)
            .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        
        BigDecimal oldSpent = budget.getCurrentSpent();
        budget.setCurrentSpent(oldSpent.add(amount));
        
        Budget savedBudget = budgetRepository.save(budget);
        
        // Check if alert should be triggered
        checkAndCreateAlert(savedBudget);
    }
    
    @Transactional
    public void recalculateBudgetSpending(Budget budget, BigDecimal totalSpent) {
        budget.setCurrentSpent(totalSpent);
        budgetRepository.save(budget);
        checkAndCreateAlert(budget);
    }
    
    private void checkAndCreateAlert(Budget budget) {
        if (!budget.shouldAlert()) {
            return;
        }
        
        String alertType = budget.isOverBudget() ? "OVER_BUDGET" : "THRESHOLD_REACHED";
        
        // Check if similar alert was sent recently (within 24 hours)
        boolean recentAlertExists = budgetAlertRepository.existsRecentAlert(
            budget.getId(),
            alertType,
            LocalDateTime.now().minusHours(24)
        );
        
        if (recentAlertExists) {
            return;
        }
        
        String message = budget.isOverBudget()
            ? String.format("Budget '%s' exceeded! Spent: $%.2f of $%.2f limit", 
                budget.getName(), budget.getCurrentSpent(), budget.getAmountLimit())
            : String.format("Budget '%s' reached %.0f%% threshold! Spent: $%.2f of $%.2f", 
                budget.getName(), budget.getUsagePercentage(), 
                budget.getCurrentSpent(), budget.getAmountLimit());
        
        BudgetAlert alert = BudgetAlert.builder()
            .budgetId(budget.getId())
            .userId(budget.getUserId())
            .alertType(alertType)
            .message(message)
            .percentageUsed(budget.getUsagePercentage())
            .amountOver(budget.isOverBudget() 
                ? budget.getCurrentSpent().subtract(budget.getAmountLimit()) 
                : null)
            .isRead(false)
            .build();
        
        budgetAlertRepository.save(alert);
        log.warn("Budget alert created: {}", message);
    }
    
    public List<Budget> getActiveBudgets(Long userId) {
        return budgetRepository.findByUserIdAndIsActiveTrue(userId);
    }
    
    public Budget getBudgetById(Long userId, Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
            .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        
        if (!budget.getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized access to budget");
        }
        
        return budget;
    }
    
    public BudgetStatusResponse getBudgetStatus(Long userId, Long budgetId) {
        Budget budget = getBudgetById(userId, budgetId);
        
        String categoryName = null;
        if (budget.getCategoryId() != null) {
            var category = categoryClient.getCategoryById(budget.getCategoryId());
            categoryName = category != null ? category.getName() : "Unknown";
        }
        
        // Calculate days remaining
        Integer daysRemaining = null;
        if (budget.getEndDate() != null) {
            daysRemaining = (int) ChronoUnit.DAYS.between(LocalDate.now(), budget.getEndDate());
        }
        
        // Calculate daily average and projection
        long daysSinceStart = ChronoUnit.DAYS.between(budget.getStartDate(), LocalDate.now()) + 1;
        BigDecimal dailyAverage = budget.getCurrentSpent()
            .divide(BigDecimal.valueOf(daysSinceStart), 2, RoundingMode.HALF_UP);
        
        BigDecimal projectedTotal = null;
        if (budget.getEndDate() != null) {
            long totalDays = ChronoUnit.DAYS.between(budget.getStartDate(), budget.getEndDate()) + 1;
            projectedTotal = dailyAverage.multiply(BigDecimal.valueOf(totalDays));
        }
        
        // Determine status
        String status = determineStatus(budget);
        
        // Get recent alerts
        List<BudgetAlert> alerts = budgetAlertRepository.findByBudgetIdOrderBySentAtDesc(budgetId);
        List<BudgetStatusResponse.Alert> recentAlerts = alerts.stream()
            .limit(5)
            .map(alert -> BudgetStatusResponse.Alert.builder()
                .type(alert.getAlertType())
                .message(alert.getMessage())
                .date(alert.getSentAt().toLocalDate())
                .isRead(alert.getIsRead())
                .build())
            .collect(Collectors.toList());
        
        return BudgetStatusResponse.builder()
            .budgetId(budget.getId())
            .name(budget.getName())
            .categoryName(categoryName)
            .amountLimit(budget.getAmountLimit())
            .currentSpent(budget.getCurrentSpent())
            .remainingAmount(budget.getRemainingAmount())
            .usagePercentage(budget.getUsagePercentage())
            .period(budget.getPeriod())
            .startDate(budget.getStartDate())
            .endDate(budget.getEndDate())
            .status(status)
            .isActive(budget.getIsActive())
            .daysRemaining(daysRemaining)
            .dailyAverageSpent(dailyAverage)
            .projectedTotal(projectedTotal)
            .recentAlerts(recentAlerts)
            .build();
    }
    
    private String determineStatus(Budget budget) {
        if (!budget.getIsActive()) {
            return "INACTIVE";
        }
        
        if (budget.isOverBudget()) {
            return "OVER_BUDGET";
        }
        
        BigDecimal usagePercentage = budget.getUsagePercentage();
        
        if (usagePercentage.compareTo(BigDecimal.valueOf(95)) >= 0) {
            return "CRITICAL";
        } else if (usagePercentage.compareTo(budget.getAlertThreshold()) >= 0) {
            return "WARNING";
        }
        
        return "ON_TRACK";
    }
    
    public List<BudgetAlert> getUnreadAlerts(Long userId) {
        return budgetAlertRepository.findByUserIdAndIsReadFalseOrderBySentAtDesc(userId);
    }
    
    @Transactional
    public void markAlertAsRead(Long userId, Long alertId) {
        BudgetAlert alert = budgetAlertRepository.findById(alertId)
            .orElseThrow(() -> new ResourceNotFoundException("Alert not found"));
        
        if (!alert.getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized access to alert");
        }
        
        alert.setIsRead(true);
        budgetAlertRepository.save(alert);
    }
}