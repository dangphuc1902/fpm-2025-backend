package com.fpm_2025.reportingservice.repository;

import com.fpm_2025.reportingservice.domain.model.CategorySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategorySummaryRepository extends JpaRepository<CategorySummary, Long> {
    
    Optional<CategorySummary> findByUserIdAndCategoryIdAndYearMonth(
        Long userId, Long categoryId, String yearMonth
    );
    
    List<CategorySummary> findByUserIdAndYearMonth(Long userId, String yearMonth);
    
    @Query("SELECT cs FROM CategorySummary cs WHERE cs.userId = :userId " +
           "AND cs.yearMonth BETWEEN :startMonth AND :endMonth " +
           "ORDER BY cs.totalAmount DESC")
    List<CategorySummary> findByUserIdAndYearMonthBetween(
        @Param("userId") Long userId,
        @Param("startMonth") String startMonth,
        @Param("endMonth") String endMonth
    );
    
    @Query("SELECT cs FROM CategorySummary cs WHERE cs.userId = :userId " +
           "AND cs.yearMonth = :yearMonth " +
           "ORDER BY cs.totalAmount DESC")
    List<CategorySummary> findTopCategoriesByMonth(
        @Param("userId") Long userId,
        @Param("yearMonth") String yearMonth
    );

    @Query("SELECT cs FROM CategorySummary cs WHERE cs.userId = :userId " +
           "AND cs.yearMonth = :yearMonth " +
           "AND cs.type = :type " +
           "ORDER BY cs.totalAmount DESC")
    List<CategorySummary> findByUserIdAndYearMonthAndType(
        @Param("userId") Long userId,
        @Param("yearMonth") String yearMonth,
        @Param("type") CategorySummary.CategorySummaryType type
    );
}