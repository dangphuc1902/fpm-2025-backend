package com.fpm_2025.wallet_service.dto.payload.request;

import com.fpm_2025.wallet_service.entity.enums.WalletType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWalletRequest {

    @Size(min = 1, max = 100, message = "Wallet name must be between 1 and 100 characters")
    private String name;

    private WalletType type;

    private String icon;

    private Boolean isActive;
}