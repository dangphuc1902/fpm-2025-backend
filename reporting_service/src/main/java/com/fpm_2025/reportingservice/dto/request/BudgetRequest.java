package com.fpm_2025.reportingservice.dto.request;

import com.fpm_2025.reportingservice.domain.valueobject.BudgetPeriod;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetRequest {
    
    @NotBlank(message = "Budget name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    private Long categoryId;
    
    private Long walletId;
    
    @NotNull(message = "Amount limit is required")
    @Positive(message = "Amount limit must be positive")
    private BigDecimal amountLimit;
    
    @NotNull(message = "Budget period is required")
    private BudgetPeriod period;
    
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    @Min(value = 1, message = "Alert threshold must be at least 1%")
    @Max(value = 100, message = "Alert threshold must not exceed 100%")
    private BigDecimal alertThreshold;
    
    private Boolean rolloverEnabled;
}