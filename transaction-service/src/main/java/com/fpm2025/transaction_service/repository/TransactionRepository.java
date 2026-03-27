package com.fpm2025.transaction_service.repository;

import com.fpm2025.transaction_service.entity.TransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    Page<TransactionEntity> findByWalletId(Long walletId, Pageable pageable);
    
    Page<TransactionEntity> findByUserId(Long userId, Pageable pageable);
    
    List<TransactionEntity> findByUserIdAndTransactionDateBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate);
    
    List<TransactionEntity> findByWalletIdAndTransactionDateBetween(Long walletId, LocalDateTime startDate, LocalDateTime endDate);
}
