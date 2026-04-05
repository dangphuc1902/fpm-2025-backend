package com.fpm2025.transaction_service.event.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Event received from notification-service via Kafka
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedNotificationEvent implements Serializable {
    private String eventType;
    private Long notificationId;
    private Long userId;
    private String bankName;
    private BigDecimal amount;
    private String type; // INCOME / EXPENSE
    private String account;
    private String note;
    private String transactionRef;
    private String balance;
    private String transactionTime;
    private String parsedAt;
}
