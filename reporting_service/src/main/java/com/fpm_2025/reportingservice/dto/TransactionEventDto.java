package com.fpm_2025.reportingservice.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionEventDto {
    private Long id;
    private Long userId;
    private Long walletId;
    private Long categoryId;
    private BigDecimal amount;
    private String currency;
    private String type; // INCOME, EXPENSE, TRANSFER
    private LocalDateTime transactionDate;
}
