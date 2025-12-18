package com.fpm_2025.reportingservice.repository;

import com.fpm_2025.reportingservice.domain.model.MonthlySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlySummaryRepository extends JpaRepository<MonthlySummary, Long> {
    
    Optional<MonthlySummary> findByUserIdAndWalletIdAndMonthStart(
        Long userId, Long walletId, LocalDate monthStart
    );
    
    Optional<MonthlySummary> findByUserIdAndMonthStartAndWalletIdIsNull(
        Long userId, LocalDate monthStart
    );
    
    List<MonthlySummary> findByUserIdAndMonthStartBetween(
        Long userId, LocalDate startDate, LocalDate endDate
    );
    
    @Query("SELECT ms FROM MonthlySummary ms WHERE ms.userId = :userId " +
           "AND ms.monthStart BETWEEN :startDate AND :endDate " +
           "AND (:walletId IS NULL OR ms.walletId = :walletId) " +
           "ORDER BY ms.monthStart DESC")
    List<MonthlySummary> findByUserIdAndDateRangeAndWallet(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("walletId") Long walletId
    );
    
    @Query("SELECT ms FROM MonthlySummary ms WHERE ms.userId = :userId " +
           "ORDER BY ms.monthStart DESC")
    List<MonthlySummary> findByUserIdOrderByMonthStartDesc(@Param("userId") Long userId);
}