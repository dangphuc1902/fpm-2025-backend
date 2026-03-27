package com.fpm_2025.wallet_service.dto.payload.response;

import com.fpm_2025.wallet_service.entity.enums.WalletPermissionLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletPermissionResponse {
    private Long id;
    private Long walletId;
    private Long userId;
    private WalletPermissionLevel permissionLevel;
    private LocalDateTime createdAt;
}
