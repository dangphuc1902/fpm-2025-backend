package com.fpm_2025.reportingservice.repository;

import com.fpm_2025.reportingservice.domain.model.Budget;
import com.fpm_2025.reportingservice.domain.valueobject.BudgetPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    
    List<Budget> findByUserIdAndIsActiveTrue(Long userId);
    
    List<Budget> findByUserIdAndCategoryIdAndIsActiveTrue(Long userId, Long categoryId);
    
    @Query("SELECT b FROM Budget b WHERE b.userId = :userId " +
           "AND b.isActive = true " +
           "AND b.startDate <= :date " +
           "AND (b.endDate IS NULL OR b.endDate >= :date)")
    List<Budget> findActiveBudgetsForDate(
        @Param("userId") Long userId,
        @Param("date") LocalDate date
    );
    
    @Query("SELECT b FROM Budget b WHERE b.userId = :userId " +
           "AND b.period = :period " +
           "AND b.isActive = true " +
           "AND b.startDate <= :today " +
           "AND (b.endDate IS NULL OR b.endDate >= :today)")
    List<Budget> findActiveBudgetsByPeriod(
        @Param("userId") Long userId,
        @Param("period") BudgetPeriod period,
        @Param("today") LocalDate today
    );
    
    @Query("SELECT b FROM Budget b WHERE b.userId = :userId " +
           "AND b.isActive = true " +
           "AND b.currentSpent >= b.amountLimit * b.alertThreshold / 100")
    List<Budget> findBudgetsNeedingAlert(@Param("userId") Long userId);
    
    @Query("SELECT b FROM Budget b WHERE b.userId = :userId " +
           "AND (:categoryId IS NULL OR b.categoryId = :categoryId) " +
           "AND (:walletId IS NULL OR b.walletId = :walletId) " +
           "AND b.period = :period " +
           "AND b.isActive = true " +
           "AND b.startDate <= :today " +
           "AND (b.endDate IS NULL OR b.endDate >= :today)")
    Optional<Budget> findActiveBudget(
        @Param("userId") Long userId,
        @Param("categoryId") Long categoryId,
        @Param("walletId") Long walletId,
        @Param("period") BudgetPeriod period,
        @Param("today") LocalDate today
    );
}