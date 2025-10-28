package com.fpm_2025.wallet_service.service;

import com.fpm_2025.wallet_service.entity.WalletEntity;
import com.fpm_2025.wallet_service.entity.enums.*;
import com.fpm_2025.wallet_service.exception.ResourceNotFoundException;
import com.fpm_2025.wallet_service.exception.DuplicateResourceException;
import com.fpm_2025.wallet_service.exception.InsufficientBalanceException;
import com.fpm_2025.wallet_service.dto.payload.request.CreateWalletRequest;
import com.fpm_2025.wallet_service.dto.payload.request.UpdateWalletRequest;
import com.fpm_2025.wallet_service.dto.payload.response.WalletResponse;
import com.fpm_2025.wallet_service.repository.WalletRepository;
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
public class WalletService {
	@Autowired
    private WalletRepository walletRepository;
	
    Logger logger  = LoggerFactory.getLogger(WalletService.class);

    @Transactional
    public WalletResponse createWallet(CreateWalletRequest request, Long userId) {
    	logger.info("Creating wallet for user: {}, wallet name: {}", userId, request.getName());

        // Check if wallet with same name exists for user
        if (walletRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new DuplicateResourceException("Wallet with name '" + request.getName() + "' already exists for this user");
        }

        WalletEntity wallet = WalletEntity.builder()
            .userId(userId)
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

    public List<WalletResponse> getUserWallets(Long userId) {
    	logger.info("Fetching all wallets for user: {}", userId);
        List<WalletEntity> wallets = walletRepository.findByUserId(userId);
        return wallets.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    public List<WalletResponse> getUserActiveWallets(Long userId) {
    	logger.info("Fetching active wallets for user: {}", userId);
        List<WalletEntity> wallets = walletRepository.findActiveWalletsByUserId(userId);
        return wallets.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    public List<WalletResponse> getUserWalletsByType(Long userId, WalletType type) {
    	logger.info("Fetching wallets by type: {} for user: {}", type, userId);
        List<WalletEntity> wallets = walletRepository.findByUserIdAndType(userId, type);
        return wallets.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    public WalletResponse getWalletById(Long walletId, Long userId) {
    	logger.info("Fetching wallet with id: {} for user: {}", walletId, userId);
        WalletEntity wallet = walletRepository.findByIdAndUserId(walletId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with id: " + walletId));
        return mapToResponse(wallet);
    }

    @Transactional
    public WalletResponse updateWallet(Long walletId, UpdateWalletRequest request, Long userId) {
    	logger.info("Updating wallet with id: {} for user: {}", walletId, userId);

        WalletEntity wallet = walletRepository.findByIdAndUserId(walletId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with id: " + walletId));

        // Check for duplicate name if name is being changed
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
            wallet.setActive(request.getIsActive());
        }

        WalletEntity updatedWallet = walletRepository.save(wallet);
        logger.info("Wallet updated successfully with id: {}", updatedWallet.getId());

        return mapToResponse(updatedWallet);
    }

    @Transactional
    public void deleteWallet(Long walletId, Long userId) {
    	logger.info("Deleting wallet with id: {} for user: {}", walletId, userId);

    	WalletEntity wallet = walletRepository.findByIdAndUserId(walletId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with id: " + walletId));

        // Check if wallet has balance
        if (wallet.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Cannot delete wallet with remaining balance. Please transfer funds first.");
        }

        walletRepository.delete(wallet);
        logger.info("Wallet deleted successfully with id: {}", walletId);
    }

    public BigDecimal getTotalBalance(Long userId) {
    	logger.info("Calculating total balance for user: {}", userId);
        BigDecimal total = walletRepository.getTotalBalanceByUserId(userId);
        return total != null ? total : BigDecimal.ZERO;
    }

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
            
            // Check if balance would go negative
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

    // Validate wallet access (used by other services)
    public boolean validateWalletAccess(Long walletId, Long userId) {
        return walletRepository.findByIdAndUserId(walletId, userId).isPresent();
    }

    // Get wallet entity (internal use)
    @Transactional(readOnly = true)
    public WalletEntity getWalletEntity(Long walletId, Long userId) {
        return walletRepository.findByIdAndUserId(walletId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with id: " + walletId));
    }

    public long getUserWalletCount(Long userId) {
        return walletRepository.countByUserId(userId);
    }

    // Mapping method
    private WalletResponse mapToResponse(WalletEntity entity) {
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
}