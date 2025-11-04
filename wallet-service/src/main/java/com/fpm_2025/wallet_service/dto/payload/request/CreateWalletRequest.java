package com.fpm_2025.wallet_service.dto.payload.request;
import com.fpm_2025.wallet_service.entity.enums.WalletType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@Getter
@Setter
public class CreateWalletRequest {

    @NotBlank(message = "Wallet name is required")
    @Size(min = 1, max = 100, message = "Wallet name must be between 1 and 100 characters")
    private String name;

    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public WalletType getType() {
		return type;
	}

	public void setType(WalletType type) {
		this.type = type;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public BigDecimal getInitialBalance() {
		return initialBalance;
	}

	public void setInitialBalance(BigDecimal initialBalance) {
		this.initialBalance = initialBalance;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}
	
	@NotNull(message = "Wallet type is required")
	@Builder.Default
    private WalletType type;

    @Size(max = 3, message = "Currency code must be 3 characters")
    private String currency = "VND";
    
    @Builder.Default
    @DecimalMin(value = "0.0", inclusive = true, message = "Initial balance cannot be negative")
    private BigDecimal initialBalance = BigDecimal.ZERO;
    
    private String icon;
}