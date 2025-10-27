package com.fpm_2025.wallet_service.dto.mapper;

import com.fpm_2025.wallet_service.dto.payload.request.CreateWalletRequest;
import com.fpm_2025.wallet_service.dto.payload.request.UpdateWalletRequest;
import com.fpm_2025.wallet_service.dto.payload.response.WalletResponse;
import com.fpm_2025.wallet_service.entity.WalletEntity;
import com.fpm_2025.wallet_service.entity.enums.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * WalletMapper - Chuyển đổi giữa Entity và DTO
 * 
 * VAI TRÒ:
 * 1. Chuyển Request DTO → Entity (để save vào DB)
 * 2. Chuyển Entity → Response DTO (để trả về client)
 * 3. Update Entity từ Request DTO (khi update)
 * 
 * LỢI ÍCH:
 * - Tách biệt DB layer và API layer
 * - Dễ thay đổi database schema mà không ảnh hưởng API
 * - Kiểm soát những field nào được expose ra ngoài
 * - Reusable code, tránh duplicate logic
 */
@Component
public class WalletMapper {

    /**
     * Chuyển CreateWalletRequest → WalletEntity
     * 
     * DÙNG KHI: User tạo wallet mới qua API
     * 
     * Flow:
     * Client gửi JSON → CreateWalletRequest → [MAPPER] → WalletEntity → Save DB
     */

    public WalletEntity toEntity(CreateWalletRequest request, Long userId) {
        if (request == null) {
            return null;
        }

        return WalletEntity.builder()
                .userId(userId)                           // Từ JWT token
                .name(request.getName())                  // Từ request
                .type(WalletType.valueOf(request.getType().toUpperCase()))
                .currency(request.getCurrency() != null ? 
                         request.getCurrency() : "VND")   // Default value
                .balance(request.getInitialBalance() != null ? 
                        request.getInitialBalance() : BigDecimal.ZERO)
                .icon(request.getIcon())
                .isActive(true)                          // Default active
                // created_at, updated_at tự động set bởi @CreationTimestamp
                .build();
    }

    /**
     * Chuyển WalletEntity → WalletResponse
     * 
     * DÙNG KHI: Trả dữ liệu về cho client
     * 
     * Flow:
     * DB Query → WalletEntity → [MAPPER] → WalletResponse → JSON → Client
     */
    public WalletResponse toResponse(WalletEntity entity) {
        if (entity == null) {
            return null;
        }

        return WalletResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .name(entity.getName())
                .type(entity.getType())
                .currency(entity.getCurrency())
                .balance(entity.getBalance())
                .icon(entity.getIcon())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Chuyển List<WalletEntity> → List<WalletResponse>
     * 
     * DÙNG KHI: Trả về danh sách wallets
     */
    public List<WalletResponse> toResponseList(List<WalletEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update Entity từ UpdateWalletRequest
     * 
     * DÙNG KHI: User update thông tin wallet
     * 
     * Flow:
     * 1. Lấy entity từ DB
     * 2. Update fields từ request
     * 3. Save lại DB
     * 
     * LƯU Ý: Chỉ update những field được gửi lên (null = không update)
     */
    public void updateEntityFromRequest(WalletEntity entity, UpdateWalletRequest request) {
        if (entity == null || request == null) {
            return;
        }

        // Chỉ update nếu field không null
        if (request.getName() != null) {
            entity.setName(request.getName());
        }

        if (request.getType() != null) {
            entity.setType(WalletType.valueOf(request.getType().toUpperCase()));
        }

        if (request.getIcon() != null) {
            entity.setIcon(request.getIcon());
        }

        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }

        // Balance KHÔNG được update qua API (chỉ qua transaction)
        // updated_at tự động update bởi @UpdateTimestamp
    }

    /**
     * Tạo Entity rỗng với default values
     * 
     * DÙNG KHI: Cần tạo wallet với minimal info
     */
    public WalletEntity createDefaultEntity(Long userId, String name, WalletType type) {
        return WalletEntity.builder()
                .userId(userId)
                .name(name)
                .type(type)
                .currency("VND")
                .balance(BigDecimal.ZERO)
                .isActive(true)
                .build();
    }
}