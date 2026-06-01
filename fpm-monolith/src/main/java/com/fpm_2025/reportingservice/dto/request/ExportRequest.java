package com.fpm_2025.reportingservice.dto.request;

import com.fpm_2025.reportingservice.domain.valueobject.ExportFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequest {
    
    @NotNull(message = "Export format is required")
    private ExportFormat format;
    
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    
    @NotNull(message = "End date is required")
    private LocalDate endDate;
    
    private Long walletId;
    
    private boolean includeCharts;
    
    private boolean includeCategories;
}