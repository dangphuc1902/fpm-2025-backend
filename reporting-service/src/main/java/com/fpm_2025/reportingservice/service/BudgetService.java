package com.fpm_2025.reportingservice.service;

import com.fpm_2025.reportingservice.domain.model.Budget;
import com.fpm_2025.reportingservice.domain.model.BudgetAlert;
import com.fpm_2025.reportingservice.dto.request.BudgetRequest;
import com.fpm_2025.reportingservice.dto.response.BudgetStatusResponse;
import com.fpm_2025.reportingservice.exception.ResourceNotFoundException;
import com.fpm_2025.reportingservice.repository.BudgetAlertRepository;
import com.fpm_2025.reportingservice.repository.BudgetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetService {
    
    private final BudgetRepository budgetRepository;
    private final BudgetAlertRepository budgetAlertRepository;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_BUDGET_ALERTS = "budget.alerts";
    
    @Transactional
    public Budget createBudget(Long userId, BudgetRequest request) {
        log.info("Creating budget for user {}, category: {}", userId, request.getCategoryName());
        
        Budget budget = Budget.builder()
            .userId(userId)
            .categoryId(request.getCategoryId())
            .categoryName(request.getCategoryName())
            .amountLimit(request.getAmountLimit())
            .period(request.getPeriod())
            .yearMonth(request.getYearMonth())
            .isActive(true)
            .amountUsed(BigDecimal.ZERO)
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
        
        budget.setCategoryName(request.getCategoryName());
        budget.setAmountLimit(request.getAmountLimit());
        
        if (request.getPeriod() != null) {
            budget.setPeriod(request.getPeriod());
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
        
        BigDecimal oldUsed = budget.getAmountUsed();
        budget.setAmountUsed(oldUsed.add(amount));
        
        Budget savedBudget = budgetRepository.save(budget);
        
        // Check if alert should be triggered
        checkAndCreateAlert(savedBudget);
    }
    
    @Transactional
    public void recalculateBudgetSpending(Budget budget, BigDecimal totalSpent) {
        budget.setAmountUsed(totalSpent);
        budgetRepository.save(budget);
        checkAndCreateAlert(budget);
    }
    
    private void checkAndCreateAlert(Budget budget) {
        BigDecimal usagePercentage = budget.getUsagePercentage();
        
        // Check thresholds: 80% and 100%
        int thresholdPercent;
        if (budget.isOverBudget()) {
            thresholdPercent = 100;
        } else if (usagePercentage.compareTo(BigDecimal.valueOf(80)) >= 0) {
            thresholdPercent = 80;
        } else {
            return; // No alert needed
        }
        
        // Check if similar alert was sent recently (within 24 hours)
        boolean recentAlertExists = budgetAlertRepository.existsRecentAlert(
            budget.getId(),
            thresholdPercent,
            LocalDateTime.now().minusHours(24)
        );
        
        if (recentAlertExists) {
            return;
        }
        
        BudgetAlert alert = BudgetAlert.builder()
            .budgetId(budget.getId())
            .userId(budget.getUserId())
            .categoryName(budget.getCategoryName())
            .thresholdPercent(thresholdPercent)
            .amountLimit(budget.getAmountLimit())
            .amountUsed(budget.getAmountUsed())
            .isRead(false)
            .build();
        
        budgetAlertRepository.save(alert);
        
        // Publish to Kafka for notification-service
        com.fpm_2025.reportingservice.dto.BudgetAlertEvent event = com.fpm_2025.reportingservice.dto.BudgetAlertEvent.builder()
            .userId(budget.getUserId())
            .budgetId(budget.getId())
            .categoryName(budget.getCategoryName())
            .thresholdPercent(thresholdPercent)
            .amountLimit(budget.getAmountLimit())
            .amountUsed(budget.getAmountUsed())
            .triggeredAt(LocalDateTime.now())
            .build();
            
        kafkaTemplate.send(TOPIC_BUDGET_ALERTS, String.valueOf(budget.getUserId()), event);
        
        log.warn("Budget alert created and published: category={}, threshold={}%, used={}/{}", 
            budget.getCategoryName(), thresholdPercent, 
            budget.getAmountUsed(), budget.getAmountLimit());
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
        
        // Determine status
        String status = determineStatus(budget);
        
        // Get recent alerts
        List<BudgetAlert> alerts = budgetAlertRepository.findByBudgetIdOrderByTriggeredAtDesc(budgetId);
        List<BudgetStatusResponse.Alert> recentAlerts = alerts.stream()
            .limit(5)
            .map(alert -> BudgetStatusResponse.Alert.builder()
                .thresholdPercent(alert.getThresholdPercent())
                .categoryName(alert.getCategoryName())
                .amountLimit(alert.getAmountLimit())
                .amountUsed(alert.getAmountUsed())
                .isRead(alert.getIsRead())
                .build())
            .collect(Collectors.toList());
        
        return BudgetStatusResponse.builder()
            .budgetId(budget.getId())
            .categoryId(budget.getCategoryId())
            .categoryName(budget.getCategoryName())
            .amountLimit(budget.getAmountLimit())
            .amountUsed(budget.getAmountUsed())
            .remainingAmount(budget.getRemainingAmount())
            .usagePercentage(budget.getUsagePercentage())
            .period(budget.getPeriod())
            .yearMonth(budget.getYearMonth())
            .status(status)
            .isActive(budget.getIsActive())
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
        } else if (usagePercentage.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return "WARNING";
        }
        
        return "ON_TRACK";
    }
    
    public List<BudgetAlert> getUnreadAlerts(Long userId) {
        return budgetAlertRepository.findByUserIdAndIsReadFalseOrderByTriggeredAtDesc(userId);
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