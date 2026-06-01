package com.fpm2025.reporting_service.dto.response;

import com.fpm2025.reporting_service.domain.valueobject.ExportFormat;
import com.fpm2025.reporting_service.domain.valueobject.ExportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportJobResponse {
    
    private String jobId;
    private ExportFormat format;
    private ExportStatus status;
    private String downloadUrl;
    private Long fileSize;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Integer progressPercentage;
}
