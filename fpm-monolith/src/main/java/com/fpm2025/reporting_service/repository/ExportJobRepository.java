package com.fpm2025.reporting_service.repository;

import com.fpm2025.reporting_service.domain.model.ExportJob;
import com.fpm2025.reporting_service.domain.valueobject.ExportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExportJobRepository extends JpaRepository<ExportJob, Long> {
    
    List<ExportJob> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<ExportJob> findByStatus(ExportStatus status);
    
    @Query("SELECT ej FROM ExportJob ej WHERE ej.userId = :userId " +
           "AND ej.status = :status " +
           "ORDER BY ej.createdAt DESC")
    List<ExportJob> findByUserIdAndStatus(
        @Param("userId") Long userId,
        @Param("status") ExportStatus status
    );
    
    @Query("SELECT ej FROM ExportJob ej WHERE ej.status = 'DONE' " +
           "AND ej.completedAt < :beforeDate")
    List<ExportJob> findOldCompletedJobs(@Param("beforeDate") LocalDateTime beforeDate);
}
