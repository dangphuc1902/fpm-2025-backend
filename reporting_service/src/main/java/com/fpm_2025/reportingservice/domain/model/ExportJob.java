package com.fpm_2025.reportingservice.domain.model;

import com.fpm_2025.reportingservice.domain.valueobject.ExportFormat;
import com.fpm_2025.reportingservice.domain.valueobject.ExportStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "export_jobs", indexes = {
    @Index(name = "idx_user_status", columnList = "user_id,status"),
    @Index(name = "idx_job_id", columnList = "job_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportJob {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "job_id", nullable = false, unique = true)
    private String jobId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false)
    private ExportFormat format;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExportStatus status;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "file_path")
    private String filePath;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = ExportStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public void markAsProcessing() {
        this.status = ExportStatus.PROCESSING;
        this.startedAt = LocalDateTime.now();
    }
    
    public void markAsCompleted(String filePath, Long fileSize) {
        this.status = ExportStatus.COMPLETED;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.completedAt = LocalDateTime.now();
    }
    
    public void markAsFailed(String errorMessage) {
        this.status = ExportStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }
}