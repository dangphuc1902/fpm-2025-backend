package com.fpm_2025.wallet_service.dto.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse<T> {
    private int statusCode = 200;
    private String message = "";
    private T data;

    public static <T> BaseResponse<T> success(T data, String message) {
        return new BaseResponse<>(200, message == null ? "" : message, data);
    }

    public static <T> BaseResponse<T> error(T data, String message) {
        return new BaseResponse<>(400, message == null ? "" : message, data);
    }
}
