package com.fpm_2025.wallet_service.dto.mapper;

import com.fpm_2025.wallet_service.dto.payload.request.CreateTransactionRequest;
import com.fpm_2025.wallet_service.dto.payload.request.UpdateTransactionRequest;
import com.fpm_2025.wallet_service.dto.payload.response.TransactionResponse;
import com.fpm_2025.wallet_service.entity.CategoryEntity;
import com.fpm_2025.wallet_service.entity.TransactionEntity;
import com.fpm_2025.wallet_service.entity.WalletEntity;
import com.fpm_2025.wallet_service.entity.enums.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TransactionMapper - Chuyển đổi Transaction Entity ↔ DTO
 * 
 * ĐẶC BIỆT: Transaction có relationships với Wallet và Category
 */
@Component
@RequiredArgsConstructor
public class TransactionMapper {

    /**
     * Chuyển CreateTransactionRequest → TransactionEntity
     * 
     * DÙNG KHI: User tạo transaction mới
     * 
     * LƯU Ý: Cần load WalletEntity và CategoryEntity từ DB trước
     */
    public TransactionEntity toEntity(CreateTransactionRequest request, 
                                     Long userId,
                                     WalletEntity wallet,
                                     CategoryEntity category) {
        if (request == null) {
            return null;
        }

        return TransactionEntity.builder()
                .userId(userId)
                .wallet(wallet)                          // JPA relationship
                .category(category)                      // JPA relationship
                .amount(request.getAmount())
                .type(CategoryType.valueOf(request.getType().toUpperCase()))
                .note(request.getNote())
                .transactionDate(request.getTransactionDate() != null ? 
                                request.getTransactionDate() : LocalDateTime.now())
                .build();
    }

    /**
     * Chuyển TransactionEntity → TransactionResponse
     * 
     * DÙNG KHI: Trả dữ liệu về client
     * 
     * LƯU Ý: Không trả về toàn bộ Wallet/Category object (chỉ trả id và name)
     */
    public TransactionResponse toResponse(TransactionEntity entity) {
        if (entity == null) {
            return null;
        }

        return TransactionResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .walletId(entity.getWallet() != null ? entity.getWallet().getId() : null)
                .walletName(entity.getWallet() != null ? entity.getWallet().getName() : null)
                .categoryId(entity.getCategory() != null ? entity.getCategory().getId() : null)
                .categoryName(entity.getCategory() != null ? entity.getCategory().getName() : null)
                .amount(entity.getAmount())
                .type(entity.getType())
                .note(entity.getNote())
                .transactionDate(entity.getTransactionDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Chuyển List<TransactionEntity> → List<TransactionResponse>
     */
    public List<TransactionResponse> toResponseList(List<TransactionEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update Entity từ UpdateTransactionRequest
     * 
     * DÙNG KHI: User update transaction
     * 
     * LƯU Ý: Wallet KHÔNG được thay đổi (business rule)
     */
    public void updateEntityFromRequest(TransactionEntity entity, 
                                       UpdateTransactionRequest request,
                                       CategoryEntity newCategory) {
        if (entity == null || request == null) {
            return;
        }

        // Update category nếu có
        if (newCategory != null) {
            entity.setCategory(newCategory);
        }

        // Update amount nếu có
        if (request.getAmount() != null) {
            // LƯU Ý: Cần recalculate wallet balance
            entity.setAmount(request.getAmount());
        }

        // Update type nếu có
        if (request.getType() != null) {
            entity.setType(CategoryType.valueOf(request.getType().toUpperCase()));
        }

        // Update note
        if (request.getNote() != null) {
            entity.setNote(request.getNote());
        }

        // Update transaction date
        if (request.getTransactionDate() != null) {
            entity.setTransactionDate(request.getTransactionDate());
        }
    }

    /**
     * Calculate total amount by type
     * 
     * DÙNG KHI: Cần tính tổng thu/chi
     */
    public java.math.BigDecimal calculateTotalByType(
            List<TransactionEntity> transactions,
            CategoryType type) {
        
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .map(TransactionEntity::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }
}