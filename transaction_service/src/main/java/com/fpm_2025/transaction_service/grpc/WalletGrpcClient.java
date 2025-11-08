package com.fpm_2025.transaction_service.grpc;

import com.fpm_2025.wallet.*;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * gRPC Client để gọi Wallet Service
 * 
 * Được sử dụng để:
 * - Validate wallet existence và ownership
 * - Get wallet balance
 * - Validate category
 */
@Service
@Slf4j
public class WalletGrpcClient {

    @GrpcClient("wallet-service")
    private WalletServiceGrpc.WalletServiceBlockingStub walletServiceStub;

    /**
     * Validate wallet và lấy thông tin balance
     */
    public WalletValidationResponse validateWallet(Long walletId, Long userId) {
        log.info("Validating wallet: {} for user: {}", walletId, userId);

        try {
            GetWalletRequest request = GetWalletRequest.newBuilder()
                .setWalletId(walletId.toString())
                .setUserId(userId.toString())
                .build();

            GetWalletResponse response = walletServiceStub.getWallet(request);

            if (response.getSuccess()) {
                Wallet wallet = response.getWallet();
                return WalletValidationResponse.builder()
                    .valid(true)
                    .walletId(Long.parseLong(wallet.getWalletId()))
                    .walletName(wallet.getName())
                    .currentBalance(BigDecimal.valueOf(wallet.getBalance()))
                    .build();
            }

            return WalletValidationResponse.builder()
                .valid(false)
                .build();

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed: {}", e.getMessage());
            return WalletValidationResponse.builder()
                .valid(false)
                .build();
        }
    }

    /**
     * Validate category existence
     */
    public CategoryValidationResponse validateCategory(Long categoryId) {
        log.info("Validating category: {}", categoryId);

        try {
            GetCategoryRequest request = GetCategoryRequest.newBuilder()
                .setCategoryId(categoryId.toString())
                .build();

            GetCategoryResponse response = walletServiceStub.getCategory(request);

            if (response.getSuccess()) {
                Category category = response.getCategory();
                return CategoryValidationResponse.builder()
                    .valid(true)
                    .categoryId(Long.parseLong(category.getCategoryId()))
                    .categoryName(category.getName())
                    .build();
            }

            return CategoryValidationResponse.builder()
                .valid(false)
                .build();

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed: {}", e.getMessage());
            return CategoryValidationResponse.builder()
                .valid(false)
                .build();
        }
    }

    // ==================== Response DTOs ====================

    @lombok.Data
    @lombok.Builder
    public static class WalletValidationResponse {
        private boolean valid;
        private Long walletId;
        private String walletName;
        private BigDecimal currentBalance;
    }

    @lombok.Data
    @lombok.Builder
    public static class CategoryValidationResponse {
        private boolean valid;
        private Long categoryId;
        private String categoryName;
    }
}