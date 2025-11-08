package com.fpm_2025.transaction_service.service;

import com.fpm_2025.transaction_service.dto.*;
import com.fpm_2025.transaction_service.entity.TransactionEntity;
import com.fpm_2025.transaction_service.event.TransactionEventPublisher;
import com.fpm_2025.transaction_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction Service Implementation
 * 
 * Features:
 * - Create/Update/Delete transactions
 * - Publish events to RabbitMQ for wallet balance update
 * - Transaction history with pagination
 * - Statistics and reporting
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionEventPublisher eventPublisher;
    private final WalletGrpcClient walletGrpcClient; // gRPC client để validate wallet

    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request, Long userId) {
        log.info("Creating transaction for user: {}, amount: {}", userId, request.getAmount());

        // 1️, Validate wallet exists via gRPC
        WalletValidationResponse walletValidation = walletGrpcClient.validateWallet(
            request.getWalletId(), userId);
        
        if (!walletValidation.isValid()) {
            throw new ResourceNotFoundException("Wallet not found or access denied");
        }

        // 2️⃣ Validate category exists via gRPC
        CategoryValidationResponse categoryValidation = walletGrpcClient.validateCategory(
            request.getCategoryId());
        
        if (!categoryValidation.isValid()) {
            throw new ResourceNotFoundException("Category not found");
        }

        // 3️⃣ Check balance for EXPENSE transactions
        if (request.getType() == CategoryType.EXPENSE) {
            if (walletValidation.getCurrentBalance().compareTo(request.getAmount()) < 0) {
                throw new InsufficientBalanceException(
                    "Insufficient balance. Current: " + walletValidation.getCurrentBalance() +
                    ", Required: " + request.getAmount()
                );
            }
        }

        // 4️⃣ Create transaction entity
        TransactionEntity transaction = TransactionEntity.builder()
            .userId(userId)
            .walletId(request.getWalletId())
            .categoryId(request.getCategoryId())
            .amount(request.getAmount())
            .type(request.getType())
            .note(request.getNote())
            .transactionDate(request.getTransactionDate() != null ? 
                            request.getTransactionDate() : LocalDateTime.now())
            .build();

        TransactionEntity savedTransaction = transactionRepository.save(transaction);

        // 5️⃣ Publish event to RabbitMQ for wallet balance update
        publishTransactionCreatedEvent(savedTransaction);

        log.info("Transaction created successfully with id: {}", savedTransaction.getId());

        return mapToResponse(savedTransaction, walletValidation, categoryValidation);
    }

    @Transactional
    public TransactionResponse updateTransaction(
            Long transactionId, 
            UpdateTransactionRequest request, 
            Long userId) {
        
        log.info("Updating transaction: {} for user: {}", transactionId, userId);

        // 1️⃣ Get existing transaction
        TransactionEntity transaction = transactionRepository
            .findByIdAndUserId(transactionId, userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Transaction not found with id: " + transactionId));

        BigDecimal oldAmount = transaction.getAmount();
        CategoryType oldType = transaction.getType();

        // 2️⃣ Update fields
        if (request.getAmount() != null) {
            transaction.setAmount(request.getAmount());
        }
        
        if (request.getType() != null) {
            transaction.setType(request.getType());
        }
        
        if (request.getCategoryId() != null) {
            // Validate new category
            CategoryValidationResponse categoryValidation = 
                walletGrpcClient.validateCategory(request.getCategoryId());
            
            if (!categoryValidation.isValid()) {
                throw new ResourceNotFoundException("Category not found");
            }
            
            transaction.setCategoryId(request.getCategoryId());
        }
        
        if (request.getNote() != null) {
            transaction.setNote(request.getNote());
        }

        TransactionEntity updatedTransaction = transactionRepository.save(transaction);

        // 3️⃣ Publish balance adjustment event if amount or type changed
        if (!oldAmount.equals(transaction.getAmount()) || 
            !oldType.equals(transaction.getType())) {
            
            publishTransactionUpdatedEvent(
                updatedTransaction, 
                oldAmount, 
                oldType
            );
        }

        log.info("Transaction updated successfully with id: {}", transactionId);

        return mapToResponse(updatedTransaction, null, null);
    }

    @Transactional
    public void deleteTransaction(Long transactionId, Long userId) {
        log.info("Deleting transaction: {} for user: {}", transactionId, userId);

        TransactionEntity transaction = transactionRepository
            .findByIdAndUserId(transactionId, userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Transaction not found with id: " + transactionId));

        // Publish event để revert wallet balance
        publishTransactionDeletedEvent(transaction);

        transactionRepository.delete(transaction);

        log.info("Transaction deleted successfully with id: {}", transactionId);
    }

    public Page<TransactionResponse> getUserTransactions(Long userId, Pageable pageable) {
        log.info("Fetching transactions for user: {}", userId);
        Page<TransactionEntity> transactions = transactionRepository
            .findByUserId(userId, pageable);
        
        return transactions.map(t -> mapToResponse(t, null, null));
    }

    public TransactionResponse getTransactionById(Long transactionId, Long userId) {
        log.info("Fetching transaction: {} for user: {}", transactionId, userId);
        
        TransactionEntity transaction = transactionRepository
            .findByIdAndUserId(transactionId, userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Transaction not found with id: " + transactionId));
        
        return mapToResponse(transaction, null, null);
    }

    public TransactionStatistics getStatistics(
            Long userId, 
            LocalDateTime startDate, 
            LocalDateTime endDate) {
        
        log.info("Calculating statistics for user: {} from {} to {}", 
            userId, startDate, endDate);

        BigDecimal totalIncome = transactionRepository
            .sumAmountByUserIdAndTypeAndDateRange(
                userId, CategoryType.INCOME, startDate, endDate);
        
        BigDecimal totalExpense = transactionRepository
            .sumAmountByUserIdAndTypeAndDateRange(
                userId, CategoryType.EXPENSE, startDate, endDate);

        long transactionCount = transactionRepository
            .countByUserIdAndDateRange(userId, startDate, endDate);

        return TransactionStatistics.builder()
            .totalIncome(totalIncome != null ? totalIncome : BigDecimal.ZERO)
            .totalExpense(totalExpense != null ? totalExpense : BigDecimal.ZERO)
            .netIncome(
                (totalIncome != null ? totalIncome : BigDecimal.ZERO)
                .subtract(totalExpense != null ? totalExpense : BigDecimal.ZERO)
            )
            .transactionCount(transactionCount)
            .startDate(startDate)
            .endDate(endDate)
            .build();
    }

    // ==================== Event Publishing ====================

    private void publishTransactionCreatedEvent(TransactionEntity transaction) {
        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
            .transactionId(transaction.getId())
            .userId(transaction.getUserId())
            .walletId(transaction.getWalletId())
            .categoryId(transaction.getCategoryId())
            .amount(transaction.getAmount())
            .type(transaction.getType().name())
            .note(transaction.getNote())
            .timestamp(transaction.getCreatedAt().toInstant(
                java.time.ZoneOffset.UTC))
            .build();

        eventPublisher.publishTransactionCreated(event);
    }

    private void publishTransactionUpdatedEvent(
            TransactionEntity transaction,
            BigDecimal oldAmount,
            CategoryType oldType) {
        
        TransactionUpdatedEvent event = TransactionUpdatedEvent.builder()
            .transactionId(transaction.getId())
            .userId(transaction.getUserId())
            .walletId(transaction.getWalletId())
            .oldAmount(oldAmount)
            .newAmount(transaction.getAmount())
            .oldType(oldType.name())
            .newType(transaction.getType().name())
            .timestamp(java.time.Instant.now())
            .build();

        eventPublisher.publishTransactionUpdated(event);
    }

    private void publishTransactionDeletedEvent(TransactionEntity transaction) {
        TransactionDeletedEvent event = TransactionDeletedEvent.builder()
            .transactionId(transaction.getId())
            .userId(transaction.getUserId())
            .walletId(transaction.getWalletId())
            .amount(transaction.getAmount())
            .type(transaction.getType().name())
            .timestamp(java.time.Instant.now())
            .build();

        eventPublisher.publishTransactionDeleted(event);
    }

    // ==================== Mapping ====================

    private TransactionResponse mapToResponse(
            TransactionEntity entity,
            WalletValidationResponse walletInfo,
            CategoryValidationResponse categoryInfo) {
        
        return TransactionResponse.builder()
            .id(entity.getId())
            .userId(entity.getUserId())
            .walletId(entity.getWalletId())
            .walletName(walletInfo != null ? walletInfo.getWalletName() : null)
            .categoryId(entity.getCategoryId())
            .categoryName(categoryInfo != null ? categoryInfo.getCategoryName() : null)
            .amount(entity.getAmount())
            .type(entity.getType())
            .note(entity.getNote())
            .transactionDate(entity.getTransactionDate())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}