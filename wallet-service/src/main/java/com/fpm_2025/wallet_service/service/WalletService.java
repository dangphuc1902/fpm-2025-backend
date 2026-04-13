package com.fpm_2025.wallet_service.service;

import com.fpm2025.domain.dto.request.ShareWalletRequest;
import com.fpm2025.domain.dto.response.WalletResponse;
import com.fpm2025.domain.dto.response.WalletPermissionResponse;
import com.fpm2025.domain.enums.*;
import com.fpm2025.domain.event.TransactionCreatedEvent;
import com.fpm_2025.wallet_service.entity.WalletEntity;
import com.fpm_2025.wallet_service.exception.ResourceNotFoundException;
import com.fpm_2025.wallet_service.exception.DuplicateResourceException;
import com.fpm_2025.wallet_service.exception.InsufficientBalanceException;
import com.fpm_2025.wallet_service.dto.payload.request.CreateWalletRequest;
import com.fpm_2025.wallet_service.dto.payload.request.UpdateWalletRequest;
import com.fpm_2025.wallet_service.repository.WalletRepository;
import com.fpm_2025.wallet_service.service.imp.WalletServiceImp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WalletService implements WalletServiceImp {
    
    @Autowired
    private WalletRepository walletRepository;
    
    @Autowired
    private com.fpm_2025.wallet_service.repository.WalletPermissionRepository walletPermissionRepository;

    Logger logger = LoggerFactory.getLogger(WalletService.class);

    @Override
    @Transactional
    public WalletResponse createWallet(CreateWalletRequest request, Long userId) {
        logger.info("Creating wallet for user: {}, wallet name: {}", userId, request.getName());

        if (walletRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new DuplicateResourceException("Wallet with name '" + request.getName() + "' already exists for this user");
        }

        WalletEntity wallet = WalletEntity.builder()
            .userId(userId)
            .familyId(request.getFamilyId())
            .name(request.getName())
            .type(request.getType())
            .currency(request.getCurrency())
            .balance(request.getInitialBalance())
            .icon(request.getIcon())
            .isActive(true)
            .build();

        WalletEntity savedWallet = walletRepository.save(wallet);
        logger.info("Wallet created successfully with id: {}", savedWallet.getId());

        return mapToResponse(savedWallet);
    }

    @Override
    public List<WalletResponse> getUserWallets(Long userId) {
        logger.info("Fetching all wallets for user: {}", userId);
        List<WalletEntity> wallets = walletRepository.findByUserId(userId);
        return wallets.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Override
    public List<WalletResponse> getSharedWallets(Long userId) {
        logger.info("Fetching shared wallets for user: {}", userId);
        List<com.fpm_2025.wallet_service.entity.WalletPermissionEntity> permissions = walletPermissionRepository.findByUserId(userId);
        return permissions.stream()
            .map(p -> mapToResponse(p.getWallet()))
            .collect(Collectors.toList());
    }

    @Override
    public List<WalletResponse> getUserActiveWallets(Long userId) {
        logger.info("Fetching active wallets for user: {}", userId);
        List<WalletEntity> wallets = walletRepository.findActiveWalletsByUserId(userId);
        return wallets.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Override
    public List<WalletResponse> getUserWalletsByType(Long userId, WalletType type) {
        logger.info("Fetching wallets by type: {} for user: {}", type, userId);
        List<WalletEntity> wallets = walletRepository.findByUserIdAndType(userId, type);
        return wallets.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Override
    public WalletResponse getWalletById(Long walletId, Long userId) {
        logger.info("Fetching wallet with id: {} for user: {}", walletId, userId);
        WalletEntity wallet = walletRepository.findByIdAndUserId(walletId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with id: " + walletId));
        return mapToResponse(wallet);
    }

    @Override
    @Transactional
    public WalletResponse toggleWallet(Long walletId, Long userId) {
        logger.info("Toggling wallet status for id: {} user: {}", walletId, userId);
        WalletEntity wallet = walletRepository.findByIdAndUserId(walletId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with id: " + walletId));

        wallet.setIsActive(!wallet.getIsActive());
        WalletEntity updatedWallet = walletRepository.save(wallet);
        logger.info("Wallet status toggled to: {}", updatedWallet.getIsActive());

        return mapToResponse(updatedWallet);
    }

    @Override
    @Transactional
    public WalletResponse updateWallet(Long walletId, UpdateWalletRequest request, Long userId) {
        logger.info("Updating wallet with id: {} for user: {}", walletId, userId);

        WalletEntity wallet = walletRepository.findByIdAndUserId(walletId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with id: " + walletId));

        if (request.getName() != null && !request.getName().equals(wallet.getName())) {
            if (walletRepository.existsByUserIdAndName(userId, request.getName())) {
                throw new DuplicateResourceException("Wallet with name '" + request.getName() + "' already exists");
            }
            wallet.setName(request.getName());
        }

        if (request.getType() != null) {
            wallet.setType(request.getType());
        }

        if (request.getIcon() != null) {
            wallet.setIcon(request.getIcon());
        }

        if (request.getIsActive() != null) {
            wallet.setIsActive(request.getIsActive());
        }

        WalletEntity updatedWallet = walletRepository.save(wallet);
        logger.info("Wallet updated successfully with id: {}", updatedWallet.getId());

        return mapToResponse(updatedWallet);
    }

    @Override
    @Transactional
    public void deleteWallet(Long walletId, Long userId) {
        logger.info("Deleting wallet with id: {} for user: {}", walletId, userId);

        WalletEntity wallet = walletRepository.findByIdAndUserId(walletId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with id: " + walletId));

        if (wallet.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Cannot delete wallet with remaining balance. Please transfer funds first.");
        }

        wallet.setIsDeleted(true);
        walletRepository.save(wallet);
        logger.info("Wallet deleted successfully with id: {}", walletId);
    }

    @Override
    public BigDecimal getTotalBalance(Long userId) {
        logger.info("Calculating total balance for user: {}", userId);
        BigDecimal total = walletRepository.getTotalBalanceByUserId(userId);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    @Transactional
    public WalletResponse updateBalance(Long walletId, Long userId, BigDecimal amount, boolean isAddition) {
        logger.info("Updating balance for wallet: {}, amount: {}, isAddition: {}", walletId, amount, isAddition);

        WalletEntity wallet = walletRepository.findByIdAndUserId(walletId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with id: " + walletId));

        BigDecimal newBalance;
        if (isAddition) {
            newBalance = wallet.getBalance().add(amount);
        } else {
            newBalance = wallet.getBalance().subtract(amount);
            
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new InsufficientBalanceException("Insufficient balance in wallet. Current: " 
                    + wallet.getBalance() + ", Required: " + amount);
            }
        }

        wallet.setBalance(newBalance);
        WalletEntity updatedWallet = walletRepository.save(wallet);
        logger.info("Balance updated successfully. New balance: {}", newBalance);

        return mapToResponse(updatedWallet);
    }

    @Override
    public boolean validateWalletAccess(Long walletId, Long userId) {
        return walletRepository.findByIdAndUserId(walletId, userId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public WalletEntity getWalletEntity(Long walletId, Long userId) {
        return walletRepository.findByIdAndUserId(walletId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with id: " + walletId));
    }

    @Override
    @Transactional
    public WalletPermissionResponse shareWallet(Long walletId, ShareWalletRequest request, Long ownerId) {
        log.info("Owner {} sharing wallet {} with email {}", ownerId, walletId, request.getEmail());
        
        walletRepository.findByIdAndUserId(walletId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found or not owned by user"));
        
        throw new UnsupportedOperationException("Email resolution not implemented in this migration turn.");
    }

    @Override
    public List<WalletPermissionResponse> getSharedUsers(Long walletId, Long ownerId) {
        walletRepository.findByIdAndUserId(walletId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found or not owned by user"));

        return walletPermissionRepository.findByWalletId(walletId).stream()
                .map(p -> WalletPermissionResponse.builder()
                        .id(p.getId())
                        .walletId(p.getWallet().getId())
                        .userId(p.getUserId())
                        .permissionLevel(p.getPermissionLevel())
                        .createdAt(p.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeShare(Long walletId, Long targetUserId, Long ownerId) {
        walletRepository.findByIdAndUserId(walletId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found or not owned by user"));

        com.fpm_2025.wallet_service.entity.WalletPermissionEntity permission = walletPermissionRepository.findByWalletIdAndUserId(walletId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Share configuration not found"));

        walletPermissionRepository.delete(permission);
    }

    @Override
    public long getUserWalletCount(Long userId) {
        return walletRepository.countByUserId(userId);
    }

    @Override
    public List<WalletResponse> getFamilyWallets(Long familyId) {
        log.info("Fetching all wallets for family: {}", familyId);
        return walletRepository.findByFamilyId(familyId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void createDefaultWallet(Long userId) {
        log.info("Creating default wallet for new user: {}", userId);
        
        if (walletRepository.existsByUserIdAndName(userId, "Ví Tiền Mặt")) {
            log.warn("Default wallet already exists for user: {}", userId);
            return;
        }

        WalletEntity wallet = WalletEntity.builder()
                .userId(userId)
                .name("Ví Tiền Mặt")
                .type(WalletType.CASH)
                .currency("VND")
                .balance(BigDecimal.ZERO)
                .icon("cash_icon")
                .isActive(true)
                .isDeleted(false)
                .build();

        walletRepository.save(wallet);
        log.info("Default wallet created for user: {}", userId);
    }

    private WalletResponse mapToResponse(WalletEntity entity) {
        return WalletResponse.builder()
            .id(entity.getId())
            .userId(entity.getUserId())
            .familyId(entity.getFamilyId())
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

    @Override
    public WalletEntity getWalletEntityByUserIdAndWalletType(Long userId, WalletType type) {
        return walletRepository.findOneByUserIdAndType(userId, type)
                .orElseThrow(() -> new ResourceNotFoundException(
                         String.format("Wallet not found for userId=%d and type=%s", userId, type)
                ));
    }

    @Override
    @Transactional
    public void updateBalance(WalletEntity wallet, BigDecimal newBalance) {
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);
    }

    @Override
    @Transactional
    public void updateBalanceFromTransaction(TransactionCreatedEvent event) {
        log.info("[WalletService] Updating balance from transactionId={} walletId={} amount={} type={}",
                event.getTransactionId(), event.getWalletId(), event.getAmount(), event.getType());

        WalletEntity wallet = walletRepository.findById(event.getWalletId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Wallet not found for id: " + event.getWalletId()));

        BigDecimal oldBalance = wallet.getBalance();
        BigDecimal newBalance;

        CategoryType type = CategoryType.valueOf(event.getType().toUpperCase());
        if (type == CategoryType.INCOME) {
            newBalance = oldBalance.add(event.getAmount());
        } else if (type == CategoryType.EXPENSE) {
            if (oldBalance.compareTo(event.getAmount()) < 0) {
                log.warn("[WalletService] Insufficient balance for walletId={}, continuing with negative balance", event.getWalletId());
                // In some cases we might allow negative balance or throw error. Keeping it simple for now.
            }
            newBalance = oldBalance.subtract(event.getAmount());
        } else {
            throw new IllegalArgumentException("Unknown transaction type: " + event.getType());
        }

        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        log.info("[WalletService] Wallet id={} balance updated from {} -> {}",
                event.getWalletId(), oldBalance, newBalance);
    }
}