package com.fpm_2025.wallet_service.service.imp;

import com.fpm2025.domain.dto.request.ShareWalletRequest;
import com.fpm2025.domain.dto.response.WalletResponse;
import com.fpm2025.domain.dto.response.WalletPermissionResponse;
import com.fpm2025.domain.enums.WalletType;
import com.fpm_2025.wallet_service.dto.payload.request.CreateWalletRequest;
import com.fpm_2025.wallet_service.dto.payload.request.UpdateWalletRequest;
import com.fpm_2025.wallet_service.entity.WalletEntity;
import com.fpm_2025.wallet_service.event.model.TransactionCreatedEvent;

import java.math.BigDecimal;
import java.util.List;

public interface WalletServiceImp {
    WalletResponse createWallet(CreateWalletRequest request, Long userId);
    List<WalletResponse> getUserWallets(Long userId);
    List<WalletResponse> getSharedWallets(Long userId);
    List<WalletResponse> getUserActiveWallets(Long userId);
    List<WalletResponse> getUserWalletsByType(Long userId, WalletType type);
    WalletResponse getWalletById(Long walletId, Long userId);
    WalletResponse toggleWallet(Long walletId, Long userId);
    WalletResponse updateWallet(Long walletId, UpdateWalletRequest request, Long userId);
    void deleteWallet(Long walletId, Long userId);
    BigDecimal getTotalBalance(Long userId);
    WalletResponse updateBalance(Long walletId, Long userId, BigDecimal amount, boolean isAddition);
    boolean validateWalletAccess(Long walletId, Long userId);
    WalletEntity getWalletEntity(Long walletId, Long userId);
    WalletPermissionResponse shareWallet(Long walletId, ShareWalletRequest request, Long ownerId);
    List<WalletPermissionResponse> getSharedUsers(Long walletId, Long ownerId);
    void removeShare(Long walletId, Long targetUserId, Long ownerId);
    long getUserWalletCount(Long userId);
    List<WalletResponse> getFamilyWallets(Long familyId);
    void createDefaultWallet(Long userId);
    WalletEntity getWalletEntityByUserIdAndWalletType(Long userId, WalletType type);
    void updateBalance(WalletEntity wallet, BigDecimal newBalance);
    void updateBalanceFromTransaction(TransactionCreatedEvent event);
}
