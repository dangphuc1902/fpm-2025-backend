package com.fpm_2025.reportingservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletData {
    private Long id;
    private String name;
    private String type;
    private BigDecimal balance;
}
