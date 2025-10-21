package com.fpm_2025.wallet_service.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonFormat;

@Getter
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum WalletType {
    CASH("cash"),
    CARD("card"),
    BANK("bank");

    private final String value;

    WalletType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static WalletType fromValue(String value) {
        for (WalletType type : WalletType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid WalletType: " + value);
    }
}
