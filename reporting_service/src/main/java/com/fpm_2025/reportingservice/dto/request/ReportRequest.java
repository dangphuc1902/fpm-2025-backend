package com.fpm_2025.reportingservice.dto.request;

import com.fpm_2025.reportingservice.domain.valueobject.ExportFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {
    
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    
    @NotNull(message = "End date is required")
    private LocalDate endDate;
    
    private Long walletId;
    
    private List<Long> categoryIds;
    
    @NotNull(message = "Export format is required")
    private ExportFormat format;
    
    private Boolean includeSummary = true;
    
    private Boolean includeTransactions = true;
    
    private Boolean includeCharts = false;
    
    private Boolean includeBudgetComparison = false;
    
    private Boolean includeTrends = false;
    
    private String reportName;
}