package com.fpm_2025.wallet_service.dto.payload.request;

import com.fpm_2025.wallet_service.entity.enums.WalletPermissionLevel;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareWalletRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Permission level is required")
    private WalletPermissionLevel permissionLevel;
}
