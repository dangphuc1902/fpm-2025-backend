package com.fpm_2025.reportingservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionData {
    private Long id;
    private Long userId;
    private Long walletId;
    private String walletName;
    private String categoryName;
    private String type; // INCOME, EXPENSE, TRANSFER
    private BigDecimal amount;
    private String note;
    private LocalDateTime transactionDate;
}
