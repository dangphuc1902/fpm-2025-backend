package com.fpm_2025.wallet_service.event.model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class WalletUpdatedEvent implements Serializable {
    private Long walletId;
    private Long userId;
    private String name;
    private String type;
    private BigDecimal balance;
    private Instant timestamp;
}