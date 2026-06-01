package com.fpm_2025.wallet_service.service;

import com.fpm2025.domain.dto.request.ShareWalletRequest;
import com.fpm2025.domain.dto.response.WalletPermissionResponse;
import com.fpm2025.domain.dto.response.WalletResponse;
import com.fpm2025.domain.enums.WalletPermissionLevel;
import com.fpm2025.domain.enums.WalletType;
import com.fpm2025.domain.event.TransactionCreatedEvent;
import com.fpm_2025.wallet_service.client.UserAuthClient;
import com.fpm_2025.wallet_service.dto.mapper.WalletMapper;
import com.fpm_2025.wallet_service.dto.payload.request.CreateWalletRequest;
import com.fpm_2025.wallet_service.dto.payload.request.UpdateWalletRequest;
import com.fpm_2025.wallet_service.entity.WalletEntity;
import com.fpm_2025.wallet_service.entity.WalletPermissionEntity;
import com.fpm_2025.wallet_service.repository.WalletPermissionRepository;
import com.fpm_2025.wallet_service.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletPermissionRepository permissionRepository;
    private final WalletMapper walletMapper;
    private final UserAuthClient userAuthClient;

    public List<WalletResponse> getUserWallets(Long userId) {
        return walletRepository.findByUserId(userId).stream()
                .map(walletMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<WalletResponse> getUserActiveWallets(Long userId) {
        return walletRepository.findActiveWalletsByUserId(userId).stream()
                .map(walletMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<WalletResponse> getUserWalletsByType(Long userId, WalletType type) {
        return walletRepository.findByUserIdAndType(userId, type).stream()
                .map(walletMapper::toResponse)
                .collect(Collectors.toList());
    }

    public WalletResponse getWalletById(Long id, Long userId) {
        return walletRepository.findByIdAndUserId(id, userId)
                .map(walletMapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
    }

    public WalletEntity getWalletEntity(Long walletId, Long userId) {
        return walletRepository.findByIdAndUserId(walletId, userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
    }

    @Transactional
    public WalletResponse createWallet(CreateWalletRequest request, Long userId) {
        if (walletRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new RuntimeException("Wallet with name " + request.getName() + " already exists");
        }
        WalletEntity entity = walletMapper.toEntity(request, userId);
        return walletMapper.toResponse(walletRepository.save(entity));
    }

    @Transactional
    public void createDefaultWallet(Long userId) {
        log.info("Creating default wallet for user: {}", userId);
        if (walletRepository.existsByUserIdAndName(userId, "Ví Tiền Mặt")) {
            return; // Idempotent
        }
        WalletEntity wallet = WalletEntity.builder()
                .userId(userId)
                .name("Ví Tiền Mặt")
                .type(WalletType.CASH)
                .balance(BigDecimal.ZERO)
                .currency("VND")
                .icon("cash_icon")
                .isActive(true)
                .isDeleted(false)
                .build();
        walletRepository.save(wallet);
    }

    @Transactional
    public WalletResponse updateWallet(Long id, UpdateWalletRequest request, Long userId) {
        WalletEntity entity = walletRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        walletMapper.updateEntityFromRequest(entity, request);
        return walletMapper.toResponse(walletRepository.save(entity));
    }

    @Transactional
    public WalletResponse toggleWallet(Long id, Long userId) {
        WalletEntity entity = walletRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        entity.setIsActive(!entity.getIsActive());
        return walletMapper.toResponse(walletRepository.save(entity));
    }

    @Transactional
    public void deleteWallet(Long id, Long userId) {
        WalletEntity entity = walletRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        entity.setIsDeleted(true);
        walletRepository.save(entity);
    }

    public BigDecimal getTotalBalance(Long userId) {
        return walletRepository.getTotalBalanceByUserId(userId);
    }

    public long getUserWalletCount(Long userId) {
        return walletRepository.countByUserId(userId);
    }

    public List<WalletResponse> getFamilyWallets(Long familyId) {
        return walletRepository.findByFamilyId(familyId).stream()
                .map(walletMapper::toResponse)
                .collect(Collectors.toList());
    }

    public boolean validateWalletAccess(Long walletId, Long userId) {
        WalletEntity wallet = walletRepository.findById(walletId).orElse(null);
        if (wallet == null) return false;
        if (wallet.getUserId().equals(userId)) return true;
        
        return permissionRepository.findByWalletIdAndUserId(walletId, userId).isPresent();
    }

    @Transactional
    public void updateBalance(Long walletId, Long userId, BigDecimal amount, boolean isAddition) {
        log.info("Updating balance for wallet {}: {} {}", walletId, (isAddition ? "+" : "-"), amount);
        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        if (isAddition) {
            wallet.setBalance(wallet.getBalance().add(amount));
        } else {
            wallet.setBalance(wallet.getBalance().subtract(amount));
        }
        walletRepository.save(wallet);
    }

    @Transactional
    public void updateBalance(WalletEntity wallet, BigDecimal newBalance) {
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);
    }

    @Transactional
    public void updateBalanceFromTransaction(TransactionCreatedEvent event) {
        log.info("Processing transaction event for wallet: {}", event.getWalletId());
        WalletEntity wallet = walletRepository.findById(event.getWalletId())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        // Phân biệt INCOME/EXPENSE từ event type (hoặc sử dụng amount trực tiếp)
        if ("INCOME".equalsIgnoreCase(event.getType())) {
            wallet.setBalance(wallet.getBalance().add(event.getAmount()));
        } else {
            wallet.setBalance(wallet.getBalance().subtract(event.getAmount()));
        }
        walletRepository.save(wallet);
    }

    @Transactional
    public WalletPermissionResponse shareWallet(Long walletId, ShareWalletRequest request, Long userId) {
        WalletEntity wallet = walletRepository.findByIdAndUserId(walletId, userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found or not owned by user"));

        Long targetUserId = userAuthClient.getUserIdByEmail(request.getEmail());
        if (targetUserId == null) {
            throw new RuntimeException("User with email " + request.getEmail() + " not found");
        }

        WalletPermissionEntity permission = permissionRepository.findByWalletIdAndUserId(walletId, targetUserId)
                .orElse(WalletPermissionEntity.builder()
                        .wallet(wallet)
                        .userId(targetUserId)
                        .build());

        permission.setPermissionLevel(request.getPermissionLevel());
        permissionRepository.save(permission);

        return WalletPermissionResponse.builder()
                .walletId(walletId)
                .userId(targetUserId)
                .permissionLevel(request.getPermissionLevel())
                .build();
    }

    public List<WalletPermissionResponse> getSharedUsers(Long walletId, Long userId) {
        walletRepository.findByIdAndUserId(walletId, userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found or not owned by user"));

        return permissionRepository.findByWalletId(walletId).stream()
                .map(p -> WalletPermissionResponse.builder()
                        .walletId(p.getWallet().getId())
                        .userId(p.getUserId())
                        .permissionLevel(p.getPermissionLevel())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeShare(Long walletId, Long targetUserId, Long userId) {
        walletRepository.findByIdAndUserId(walletId, userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found or not owned by user"));

        permissionRepository.findByWalletIdAndUserId(walletId, targetUserId)
                .ifPresent(permissionRepository::delete);
    }

    public List<WalletResponse> getSharedWallets(Long userId) {
        List<Long> sharedWalletIds = permissionRepository.findByUserId(userId).stream()
                .map(p -> p.getWallet().getId())
                .collect(Collectors.toList());
        
        return walletRepository.findAllById(sharedWalletIds).stream()
                .map(walletMapper::toResponse)
                .collect(Collectors.toList());
    }
}
