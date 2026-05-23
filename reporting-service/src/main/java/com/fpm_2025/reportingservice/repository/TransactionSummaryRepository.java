package com.fpm_2025.reportingservice.repository;

import com.fpm_2025.reportingservice.entity.TransactionSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionSummaryRepository extends JpaRepository<TransactionSummaryEntity, Long> {
    java.util.Optional<TransactionSummaryEntity> findByUserIdAndPeriod(Long userId, String period);
}
