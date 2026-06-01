package com.fpm2025.transaction_service.service;

import com.fpm2025.domain.dto.request.BankNotificationRequest;
import com.fpm2025.domain.dto.request.TransactionRequest;
import com.fpm2025.domain.dto.request.UpdateTransactionRequest;
import com.fpm2025.domain.dto.response.TransactionResponse;
import com.fpm2025.domain.enums.CategoryType;
import com.fpm2025.transaction_service.entity.TransactionEntity;
import com.fpm2025.transaction_service.entity.enums.TransactionStatus;
import com.fpm2025.transaction_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.context.ApplicationEventPublisher;
import com.fpm2025.domain.event.TransactionCreatedEvent;
import com.fpm_2025.wallet_service.service.WalletService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletService walletService;
    private final ApplicationEventPublisher eventPublisher;
    private final com.fpm2025.transaction_service.repository.TransactionAttachmentRepository attachmentRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            WalletService walletService,
            ApplicationEventPublisher eventPublisher,
            com.fpm2025.transaction_service.repository.TransactionAttachmentRepository attachmentRepository) {
        this.transactionRepository = transactionRepository;
        this.walletService = walletService;
        this.eventPublisher = eventPublisher;
        this.attachmentRepository = attachmentRepository;
    }

    @Transactional
    public TransactionResponse createTransaction(Long userId, TransactionRequest request) {
        log.info("Creating transaction for user {} in wallet {}", userId, request.getWalletId());

        try {
            boolean isAddition = request.getType() != CategoryType.EXPENSE;
            walletService.updateBalance(request.getWalletId(), userId, request.getAmount(), isAddition);
            log.info("Direct call: Balance updated successfully for wallet: {}", request.getWalletId());
        } catch (Exception e) {
            log.error("Direct call: Failed to update balance in Wallet Service", e);
            throw new RuntimeException("Failed to update wallet balance: " + e.getMessage());
        }

        TransactionEntity entity = TransactionEntity.builder()
                .userId(userId)
                .walletId(request.getWalletId())
                .categoryId(request.getCategoryId())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                .type(request.getType())
                .transactionDate(request.getTransactionDate() != null ? request.getTransactionDate() : LocalDateTime.now())
                .description(request.getDescription())
                .note(request.getNote())
                .location(request.getLocation())
                .isRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false)
                .status(TransactionStatus.COMPLETED)
                .build();

        TransactionEntity saved = transactionRepository.save(entity);

        publishTransactionEvent("transaction.created", userId, saved);

        return mapToResponse(saved);
    }

    public TransactionResponse getTransaction(Long userId, Long transactionId) {
        TransactionEntity entity = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new RuntimeException("Transaction not found or access denied"));
        return mapToResponse(entity);
    }

    public Page<TransactionResponse> getTransactionsByWallet(Long userId, Long walletId, int page, int size) {
        Page<TransactionEntity> transactions = transactionRepository.findByWalletId(
                walletId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate")));
        return transactions.map(this::mapToResponse);
    }

    public Page<TransactionEntity> findByWalletIdRaw(Long walletId, int page, int size) {
        return transactionRepository.findByWalletId(
                walletId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate")));
    }

    public TransactionEntity findById(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id: " + transactionId));
    }

    public List<TransactionEntity> findByUserAndDateRange(Long userId, LocalDateTime start, LocalDateTime end, List<Long> walletIds) {
        if (walletIds == null || walletIds.isEmpty()) {
            return transactionRepository.findByUserIdAndTransactionDateBetween(userId, start, end);
        }
        return transactionRepository.findByUserIdAndDateRangeAndWallets(userId, start, end, walletIds);
    }

    public BigDecimal sumExpense(Long userId, LocalDateTime start, LocalDateTime end, Long categoryId) {
        return transactionRepository.sumExpenseByUserAndDateRange(userId, start, end, categoryId);
    }

    public Page<TransactionResponse> listTransactions(
            Long userId,
            Long walletId,
            Long categoryId,
            CategoryType type,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page, int size) {

        Page<TransactionEntity> result = transactionRepository.findByFilters(
                userId, walletId, categoryId, type, startDate, endDate,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate")));

        return result.map(this::mapToResponse);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long userId, Long transactionId, UpdateTransactionRequest request) {
        log.info("Updating transaction {} for user {}", transactionId, userId);

        TransactionEntity entity = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new RuntimeException("Transaction not found or access denied"));

        boolean balanceChanged = (request.getAmount() != null && !request.getAmount().equals(entity.getAmount()))
                || (request.getType() != null && request.getType() != entity.getType());

        if (balanceChanged) {
            revertWalletBalance(entity);
            applyWalletBalance(
                    entity.getWalletId(),
                    request.getAmount() != null ? request.getAmount() : entity.getAmount(),
                    request.getType() != null ? request.getType() : entity.getType(),
                    request.getCurrency() != null ? request.getCurrency() : entity.getCurrency(),
                    "Update transaction #" + transactionId);
        }

        if (request.getAmount() != null)          entity.setAmount(request.getAmount());
        if (request.getCurrency() != null)         entity.setCurrency(request.getCurrency());
        if (request.getType() != null)             entity.setType(request.getType());
        if (request.getCategoryId() != null)       entity.setCategoryId(request.getCategoryId());
        if (request.getTransactionDate() != null)  entity.setTransactionDate(request.getTransactionDate());
        if (request.getDescription() != null)      entity.setDescription(request.getDescription());
        if (request.getNote() != null)             entity.setNote(request.getNote());
        if (request.getLocation() != null)         entity.setLocation(request.getLocation());
        if (request.getIsRecurring() != null)      entity.setIsRecurring(request.getIsRecurring());

        TransactionEntity updated = transactionRepository.save(entity);
        try {
            eventPublisher.publishEvent(Map.of(
                    "topic", "transaction.updated",
                    "userId", userId,
                    "transactionId", updated.getId(),
                    "amount", updated.getAmount()
            ));
        } catch (Exception e) {
            log.error("EventPublisher: Failed to publish transaction.updated event", e);
        }

        return mapToResponse(updated);
    }

    @Transactional
    public void deleteTransaction(Long userId, Long transactionId) {
        log.info("Deleting transaction {} for user {}", transactionId, userId);

        TransactionEntity entity = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new RuntimeException("Transaction not found or access denied"));

        revertWalletBalance(entity);
        transactionRepository.delete(entity);

        try {
            eventPublisher.publishEvent(Map.of(
                    "topic", "transaction.deleted",
                    "userId", userId,
                    "transactionId", transactionId,
                    "amount", entity.getAmount()
            ));
        } catch (Exception e) {
            log.error("EventPublisher: Failed to publish transaction.deleted event", e);
        }
    }

    @Transactional
    public com.fpm2025.transaction_service.entity.TransactionAttachmentEntity uploadAttachment(
            Long userId, Long transactionId, org.springframework.web.multipart.MultipartFile file) {
        
        transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new RuntimeException("Transaction not found or access denied"));

        String fileUrl = "https://fpm-storage.local/uploads/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        
        com.fpm2025.transaction_service.entity.TransactionAttachmentEntity attachment = 
            com.fpm2025.transaction_service.entity.TransactionAttachmentEntity.builder()
                .transactionId(transactionId)
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileUrl(fileUrl)
                .fileSize(file.getSize())
                .build();
                
        return attachmentRepository.save(attachment);
    }

    @Transactional
    public void deleteAttachment(Long userId, Long transactionId, Long attachmentId) {
        transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new RuntimeException("Transaction not found or access denied"));
        attachmentRepository.deleteById(attachmentId);
    }

    private void revertWalletBalance(TransactionEntity entity) {
        try {
            walletService.updateBalance(entity.getWalletId(), entity.getUserId(), entity.getAmount(), entity.getType() == CategoryType.EXPENSE);
        } catch (Exception e) {
            log.error("Direct call: Failed to revert wallet balance", e);
        }
    }

    private void applyWalletBalance(Long walletId, java.math.BigDecimal amount,
                                     CategoryType type, String currency, String desc) {
        try {
            walletService.updateBalance(walletId, null, amount, type != CategoryType.EXPENSE);
        } catch (Exception e) {
            log.error("Direct call: Failed to apply wallet balance", e);
        }
    }

    private void publishTransactionEvent(String topic, Long userId, TransactionEntity saved) {
        try {
            TransactionCreatedEvent event = TransactionCreatedEvent.builder()
                    .transactionId(saved.getId())
                    .walletId(saved.getWalletId())
                    .userId(saved.getUserId())
                    .categoryId(saved.getCategoryId())
                    .amount(saved.getAmount())
                    .type(saved.getType().name())
                    .note(saved.getNote())
                    .timestamp(java.time.Instant.now())
                    .build();
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("EventPublisher: Failed to publish {} event", topic, e);
        }
    }



    public TransactionResponse mapToResponse(TransactionEntity entity) {
        return TransactionResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .walletId(entity.getWalletId())
                .categoryId(entity.getCategoryId())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .type(entity.getType())
                .description(entity.getDescription())
                .note(entity.getNote())
                .location(entity.getLocation())
                .isRecurring(entity.getIsRecurring())
                .transactionDate(entity.getTransactionDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Transactional
    public TransactionResponse processBankNotification(Long userId, BankNotificationRequest request) {
        return null;
    }
}
