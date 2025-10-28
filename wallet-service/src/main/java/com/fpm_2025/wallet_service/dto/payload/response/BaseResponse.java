package com.fpm_2025.wallet_service.dto.payload.response;

public class BaseResponse<T> {
    private int statusCode = 200;
    private String message = "";
    private T data;

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    // Static factory for convenience
    public static <T> BaseResponse<T> success(T data, String message) {
        BaseResponse<T> resp = new BaseResponse<>();
        resp.setStatusCode(200);
        resp.setMessage(message == null ? "" : message);
        resp.setData(data);
        return resp;
    }

    // Note: use the static factory method above: BaseResponse.success(data, message)
}
