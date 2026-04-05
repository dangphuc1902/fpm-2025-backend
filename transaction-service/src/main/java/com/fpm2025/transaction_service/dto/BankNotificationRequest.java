package com.fpm2025.transaction_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankNotificationRequest {
    private String bankName;
    private BigDecimal amount;
    private String type; // INCOME / EXPENSE
    private String account;
    private String note;
    private String transactionRef;
    private String balance;
    private String transactionTime;
}
