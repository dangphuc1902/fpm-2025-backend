package com.fpm2025.reporting_service.repository;

import com.fpm2025.reporting_service.entity.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<ReportEntity, Long> {
    List<ReportEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<ReportEntity> findByIdAndUserId(Long id, Long userId);
}

