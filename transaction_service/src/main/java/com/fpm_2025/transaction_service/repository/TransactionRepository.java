package com.fpm_2025.transaction_service.repository;

import com.fpm_2025.transaction_service.entity.TransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.fpm2025.core.dto.response.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    // Basic queries
    Optional<TransactionEntity> findByIdAndUserId(Long id, Long userId);
    Page<TransactionEntity> findByUserId(Long userId, Pageable pageable);
    Page<TransactionEntity> findByUserIdAndType(Long userId, CategoryType type, Pageable pageable);
    
    // Wallet-based queries
    Page<TransactionEntity> findByWalletIdAndUserId(Long walletId, Long userId, Pageable pageable);
    List<TransactionEntity> findByWalletId(Long walletId);
    
    // Category-based queries
    List<TransactionEntity> findByCategoryIdAndUserId(Long categoryId, Long userId);
    
    // Date range queries
    @Query("SELECT t FROM TransactionEntity t " +
           "WHERE t.userId = :userId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate DESC")
    List<TransactionEntity> findByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    // Statistics queries
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionEntity t " +
           "WHERE t.userId = :userId " +
           "AND t.type = :type " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByUserIdAndTypeAndDateRange(
        @Param("userId") Long userId,
        @Param("type") CategoryType type,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT COUNT(t) FROM TransactionEntity t " +
           "WHERE t.userId = :userId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate")
    long countByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    long countByUserId(Long userId);
    
    // Category spending breakdown
    @Query("SELECT t.category.name, SUM(t.amount) " +
           "FROM TransactionEntity t " +
           "WHERE t.userId = :userId " +
           "AND t.type = 'EXPENSE' " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY t.category.name " +
           "ORDER BY SUM(t.amount) DESC")
    List<Object[]> findCategorySpendingBreakdown(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    // Latest transactions
    @Query("SELECT t FROM TransactionEntity t " +
           "WHERE t.userId = :userId " +
           "ORDER BY t.transactionDate DESC")
    List<TransactionEntity> findLatestByUserId(
        @Param("userId") Long userId,
        Pageable pageable
    );
}