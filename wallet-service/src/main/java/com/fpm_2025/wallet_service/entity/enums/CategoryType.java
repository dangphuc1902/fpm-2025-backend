package com.fpm_2025.wallet_service.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

@Getter
@RequiredArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum CategoryType {
    EXPENSE("expense"),
    INCOME("income");

    private final String value;
    
    CategoryType(String string) {
		this.value = "";	
	}

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CategoryType fromValue(String value) {
        for (CategoryType type : CategoryType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid CategoryType: " + value);
    }
}