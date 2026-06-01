package com.fpm_2025.reportingservice.repository;

import com.fpm_2025.reportingservice.domain.model.MonthlySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlySummaryRepository extends JpaRepository<MonthlySummary, Long> {
    
    Optional<MonthlySummary> findByUserIdAndYearMonth(Long userId, String yearMonth);
    
    List<MonthlySummary> findByUserIdOrderByYearMonthDesc(Long userId);
    
    @Query("SELECT ms FROM MonthlySummary ms WHERE ms.userId = :userId " +
           "AND ms.yearMonth BETWEEN :startMonth AND :endMonth " +
           "ORDER BY ms.yearMonth DESC")
    List<MonthlySummary> findByUserIdAndYearMonthBetween(
        @Param("userId") Long userId,
        @Param("startMonth") String startMonth,
        @Param("endMonth") String endMonth
    );
}