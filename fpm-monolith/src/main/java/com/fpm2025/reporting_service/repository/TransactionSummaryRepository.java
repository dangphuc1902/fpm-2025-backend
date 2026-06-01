package com.fpm2025.reporting_service.repository;

import com.fpm2025.reporting_service.entity.TransactionSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionSummaryRepository extends JpaRepository<TransactionSummaryEntity, Long> {
    java.util.Optional<TransactionSummaryEntity> findByUserIdAndPeriod(Long userId, String period);
}

