package com.fpm_2025.reportingservice.repository;

import com.fpm_2025.reportingservice.domain.model.ExportJob;
import com.fpm_2025.reportingservice.domain.valueobject.ExportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExportJobRepository extends JpaRepository<ExportJob, Long> {
    
    Optional<ExportJob> findByJobId(String jobId);
    
    List<ExportJob> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<ExportJob> findByStatus(ExportStatus status);
    
    @Query("SELECT ej FROM ExportJob ej WHERE ej.userId = :userId " +
           "AND ej.status = :status " +
           "ORDER BY ej.createdAt DESC")
    List<ExportJob> findByUserIdAndStatus(
        @Param("userId") Long userId,
        @Param("status") ExportStatus status
    );
    
    @Query("SELECT ej FROM ExportJob ej WHERE ej.status = 'COMPLETED' " +
           "AND ej.completedAt < :beforeDate")
    List<ExportJob> findOldCompletedJobs(@Param("beforeDate") LocalDateTime beforeDate);
}