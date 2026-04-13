package com.fpm2025.transaction_service.repository;

import com.fpm2025.transaction_service.entity.TransactionEntity;
import com.fpm2025.domain.enums.CategoryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long>,
        JpaSpecificationExecutor<TransactionEntity> {

    // Basic finders
    Page<TransactionEntity> findByWalletId(Long walletId, Pageable pageable);
    Page<TransactionEntity> findByUserId(Long userId, Pageable pageable);

    // Date range
    List<TransactionEntity> findByUserIdAndTransactionDateBetween(
            Long userId, LocalDateTime startDate, LocalDateTime endDate);
    List<TransactionEntity> findByWalletIdAndTransactionDateBetween(
            Long walletId, LocalDateTime startDate, LocalDateTime endDate);

    // Find by userId and ID (ownership check)
    Optional<TransactionEntity> findByIdAndUserId(Long id, Long userId);

    // Dynamic filter query — supports optional params (null = no filter)
    @Query("""
        SELECT t FROM TransactionEntity t
        WHERE t.userId = :userId
          AND (:walletId IS NULL OR t.walletId = :walletId)
          AND (:categoryId IS NULL OR t.categoryId = :categoryId)
          AND (:type IS NULL OR t.type = :type)
          AND (:startDate IS NULL OR t.transactionDate >= :startDate)
          AND (:endDate IS NULL OR t.transactionDate <= :endDate)
        ORDER BY t.transactionDate DESC
        """)
    Page<TransactionEntity> findByFilters(
            @Param("userId") Long userId,
            @Param("walletId") Long walletId,
            @Param("categoryId") Long categoryId,
            @Param("type") CategoryType type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // For gRPC: date range by walletIds
    @Query("""
        SELECT t FROM TransactionEntity t
        WHERE t.userId = :userId
          AND t.transactionDate BETWEEN :startDate AND :endDate
          AND (:#{#walletIds == null || #walletIds.isEmpty()} = true OR t.walletId IN :walletIds)
        ORDER BY t.transactionDate DESC
        """)
    List<TransactionEntity> findByUserIdAndDateRangeAndWallets(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("walletIds") List<Long> walletIds);

    // For gRPC: total spending
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM TransactionEntity t
        WHERE t.userId = :userId
          AND t.type = 'EXPENSE'
          AND t.transactionDate BETWEEN :startDate AND :endDate
          AND (:categoryId IS NULL OR t.categoryId = :categoryId)
        """)
    BigDecimal sumExpenseByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("categoryId") Long categoryId);
}
