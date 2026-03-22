package com.fpm_2025.reportingservice.repository;

import com.fpm_2025.reportingservice.entity.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<ReportEntity, Long> {
    List<ReportEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<ReportEntity> findByIdAndUserId(Long id, Long userId);
}
