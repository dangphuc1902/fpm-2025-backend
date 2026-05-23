package com.fpm_2025.reportingservice.repository;

import com.fpm_2025.reportingservice.domain.model.BudgetAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BudgetAlertRepository extends JpaRepository<BudgetAlert, Long> {
    
    List<BudgetAlert> findByUserIdAndIsReadFalseOrderByTriggeredAtDesc(Long userId);
    
    List<BudgetAlert> findByUserIdOrderByTriggeredAtDesc(Long userId);
    
    List<BudgetAlert> findByBudgetIdOrderByTriggeredAtDesc(Long budgetId);
    
    @Query("SELECT ba FROM BudgetAlert ba WHERE ba.userId = :userId " +
           "AND ba.triggeredAt >= :since ORDER BY ba.triggeredAt DESC")
    List<BudgetAlert> findRecentAlerts(
        @Param("userId") Long userId,
        @Param("since") LocalDateTime since
    );
    
    @Query("SELECT COUNT(ba) > 0 FROM BudgetAlert ba " +
           "WHERE ba.budgetId = :budgetId " +
           "AND ba.thresholdPercent = :thresholdPercent " +
           "AND ba.triggeredAt >= :since")
    boolean existsRecentAlert(
        @Param("budgetId") Long budgetId,
        @Param("thresholdPercent") Integer thresholdPercent,
        @Param("since") LocalDateTime since
    );
}