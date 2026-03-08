package com.fpm_2025.reportingservice.domain.model;

import com.fpm_2025.reportingservice.domain.valueobject.ExportFormat;
import com.fpm_2025.reportingservice.domain.valueobject.ExportStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "export_jobs", indexes = {
    @Index(name = "idx_export_user_id", columnList = "user_id"),
    @Index(name = "idx_export_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportJob {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 10)
    private ExportFormat format;
    
    @Column(name = "period", nullable = false, length = 7)
    private String period;
    
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExportStatus status = ExportStatus.PENDING;
    
    @Column(name = "file_name", length = 255)
    private String fileName;
    
    @Column(name = "file_url", length = 500)
    private String fileUrl;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "error_msg", length = 500)
    private String errorMsg;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ExportStatus.PENDING;
        }
    }
    
    public void markAsProcessing() {
        this.status = ExportStatus.PROCESSING;
    }
    
    public void markAsDone(String fileName, String fileUrl, Long fileSize) {
        this.status = ExportStatus.DONE;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.fileSize = fileSize;
        this.completedAt = LocalDateTime.now();
    }
    
    public void markAsFailed(String errorMsg) {
        this.status = ExportStatus.FAILED;
        this.errorMsg = errorMsg;
        this.completedAt = LocalDateTime.now();
    }
}