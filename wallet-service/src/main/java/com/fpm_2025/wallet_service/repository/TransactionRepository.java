package com.fpm_2025.wallet_service.repository;

import com.fpm_2025.wallet_service.entity.TransactionEntity;
import com.fpm_2025.wallet_service.entity.enums.CategoryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository cho TransactionEntity.
 * <p>
 * Các method được Spring Data JPA tự động sinh query dựa trên tên method.
 * Một số method phức tạp (tổng hợp, range ngày) dùng @Query.
 * </p>
 */
@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    // 1. Tìm kiếm theo userId
    Page<TransactionEntity> findByUserId(Long userId, Pageable pageable);

    Page<TransactionEntity> findByUserIdAndType(Long userId, CategoryType type, Pageable pageable);

    long countByUserId(Long userId);

    // 2. Tìm kiếm theo ID + userId (security check)
    Optional<TransactionEntity> findByIdAndUserId(Long id, Long userId);

    // 3. Tìm kiếm theo ví (walletId)
    Page<TransactionEntity> findByWalletId(Long walletId, Pageable pageable);

    // 4. Tìm kiếm theo danh mục (categoryId) + userId
    List<TransactionEntity> findByCategoryIdAndUserId(Long categoryId, Long userId);

    // 5. Tìm kiếm theo khoảng thời gian (date range)
    List<TransactionEntity> findByUserIdAndTransactionDateBetween(
            Long userId,
            LocalDateTime startDate,
            LocalDateTime endDate);

    /**
     * Phương thức thay thế cho tên dài: findByUserIdAndDateRange
     */
    default List<TransactionEntity> findByUserIdAndDateRange(Long userId,
                                                             LocalDateTime start,
                                                             LocalDateTime end) {
        return findByUserIdAndTransactionDateBetween(userId, start, end);
    }

    // 6. Tính tổng amount theo user + type + khoảng thời gian
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM TransactionEntity t
            WHERE t.userId = :userId
              AND t.type = :type
              AND t.transactionDate BETWEEN :startDate AND :endDate
            """)
    BigDecimal sumAmountByUserIdAndTypeAndDateRange(
            @Param("userId") Long userId,
            @Param("type") CategoryType type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}