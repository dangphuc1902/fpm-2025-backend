package com.fpm_2025.reportingservice.repository;

import com.fpm_2025.reportingservice.domain.model.Budget;
import com.fpm_2025.reportingservice.domain.valueobject.BudgetPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    
    List<Budget> findByUserIdAndIsActiveTrue(Long userId);
    
    List<Budget> findByUserIdAndCategoryIdAndIsActiveTrue(Long userId, Long categoryId);
    
    Optional<Budget> findByUserIdAndCategoryIdAndYearMonth(
        Long userId, Long categoryId, String yearMonth
    );
    
    @Query("SELECT b FROM Budget b WHERE b.userId = :userId " +
           "AND b.yearMonth = :yearMonth " +
           "AND b.isActive = true")
    List<Budget> findActiveBudgetsByYearMonth(
        @Param("userId") Long userId,
        @Param("yearMonth") String yearMonth
    );
    
    @Query("SELECT b FROM Budget b WHERE b.userId = :userId " +
           "AND b.period = :period " +
           "AND b.isActive = true " +
           "AND b.yearMonth = :yearMonth")
    List<Budget> findActiveBudgetsByPeriod(
        @Param("userId") Long userId,
        @Param("period") BudgetPeriod period,
        @Param("yearMonth") String yearMonth
    );
    
    @Query("SELECT b FROM Budget b WHERE b.userId = :userId " +
           "AND b.isActive = true " +
           "AND b.amountUsed >= b.amountLimit * 0.8")
    List<Budget> findBudgetsNeedingAlert(@Param("userId") Long userId);
}