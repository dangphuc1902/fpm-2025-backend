package com.fpm_2025.reportingservice.repository;

import com.fpm_2025.reportingservice.domain.model.CategorySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategorySummaryRepository extends JpaRepository<CategorySummary, Long> {
    
    Optional<CategorySummary> findByUserIdAndCategoryIdAndMonthStart(
        Long userId, Long categoryId, LocalDate monthStart
    );
    
    List<CategorySummary> findByUserIdAndMonthStart(Long userId, LocalDate monthStart);
    
    @Query("SELECT cs FROM CategorySummary cs WHERE cs.userId = :userId " +
           "AND cs.monthStart BETWEEN :startDate AND :endDate " +
           "ORDER BY cs.totalAmount DESC")
    List<CategorySummary> findByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    @Query("SELECT cs FROM CategorySummary cs WHERE cs.userId = :userId " +
           "AND cs.monthStart = :monthStart " +
           "ORDER BY cs.totalAmount DESC")
    List<CategorySummary> findTopCategoriesByMonth(
        @Param("userId") Long userId,
        @Param("monthStart") LocalDate monthStart
    );
    
    @Query("SELECT cs FROM CategorySummary cs WHERE cs.userId = :userId " +
           "AND cs.budgetLimit IS NOT NULL " +
           "AND cs.percentageOfBudget > 80 " +
           "AND cs.monthStart = :monthStart")
    List<CategorySummary> findOverBudgetCategories(
        @Param("userId") Long userId,
        @Param("monthStart") LocalDate monthStart
    );
}