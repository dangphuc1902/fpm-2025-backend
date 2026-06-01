package com.fpm2025.reporting_service.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendData {
    private String month;
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal net;
}

