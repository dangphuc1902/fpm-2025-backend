package com.fpm_2025.wallet_service.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> validationErrors;
// // --- Builder ---
//    public static class Builder {
//        private LocalDateTime timestamp;
//        private int status;
//        private String error;
//        private String message;
//        private String path;
//
//        public Builder timestamp(LocalDateTime timestamp) {
//            this.timestamp = timestamp;
//            return this;
//        }
//
//        public Builder status(int status) {
//            this.status = status;
//            return this;
//        }
//
//        public Builder error(String error) {
//            this.error = error;
//            return this;
//        }
//
//        public Builder message(String message) {
//            this.message = message;
//            return this;
//        }
//
//        public Builder path(String path) {
//            this.path = path;
//            return this;
//        }
//
//        public ErrorResponse build() {
//            ErrorResponse response = new ErrorResponse();
//            response.timestamp = this.timestamp;
//            response.status = this.status;
//            response.error = this.error;
//            response.message = this.message;
//            response.path = this.path;
//            return response;
//        }
//    }
//
//    public static Builder builder() {
//        return new Builder();
//    }
}