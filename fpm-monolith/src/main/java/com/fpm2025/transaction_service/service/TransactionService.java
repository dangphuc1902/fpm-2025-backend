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
import com.fpm2025.wallet_service.service.WalletService;

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
    private final com.fpm2025.wallet_service.repository.CategoryRepository categoryRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            WalletService walletService,
            ApplicationEventPublisher eventPublisher,
            com.fpm2025.transaction_service.repository.TransactionAttachmentRepository attachmentRepository,
            com.fpm2025.wallet_service.repository.CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.walletService = walletService;
        this.eventPublisher = eventPublisher;
        this.attachmentRepository = attachmentRepository;
        this.categoryRepository = categoryRepository;
    }

    /** Uu tien categoryId; neu khong co thi tra theo ten category he thong. Khong tim thay -> null. */
    private Long resolveCategoryId(Long categoryId, String categoryName) {
        if (categoryId != null) return categoryId;
        if (categoryName == null || categoryName.isBlank()) return null;
        return categoryRepository.findFirstByNameAndUserIdIsNull(categoryName.trim())
                .map(com.fpm2025.wallet_service.entity.CategoryEntity::getId)
                .orElseGet(() -> {
                    log.warn("Khong tim thay category '{}', luu giao dich khong co category", categoryName);
                    return null;
                });
    }

    /** Giao dich ghi tay tu app (API). */
    @Transactional
    public TransactionResponse createTransaction(Long userId, TransactionRequest request) {
        return createTransaction(userId, request, false);
    }

    /**
     * Cach A (quyet dinh 25/10):
     * - Ghi tay: chi vuot so tien trong hu -> tu choi 400 (BR-WALLET-02, BR-TXN-02).
     * - Tu dong tu thong bao ngan hang: khoan da xay ra o ngan hang -> luon ghi, hu am thi gui canh bao.
     */
    @Transactional
    public TransactionResponse createTransaction(Long userId, TransactionRequest request, boolean fromBankNotification) {
        log.info("Creating transaction for user {} in wallet {} (auto={})", userId, request.getWalletId(), fromBankNotification);

        if (!walletService.validateWalletAccess(request.getWalletId(), userId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Bạn không có quyền ghi giao dịch vào ví " + request.getWalletId());
        }

        if (!fromBankNotification && request.getType() == CategoryType.EXPENSE) {
            var wallet = walletService.getWalletOrThrow(request.getWalletId());
            if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        String.format("Hũ \"%s\" không đủ tiền: còn %s, cần %s",
                                wallet.getName(), vnd(wallet.getBalance()), vnd(request.getAmount())));
            }
        }

        try {
            boolean isAddition = request.getType() != CategoryType.EXPENSE;
            walletService.updateBalance(request.getWalletId(), userId, request.getAmount(), isAddition);
            log.info("Direct call: Balance updated successfully for wallet: {}", request.getWalletId());
        } catch (Exception e) {
            log.error("Direct call: Failed to update balance in Wallet Service", e);
            throw new RuntimeException("Failed to update wallet balance: " + e.getMessage());
        }

        if (fromBankNotification) {
            var wallet = walletService.getWalletOrThrow(request.getWalletId());
            if (wallet.getBalance().signum() < 0) {
                // Dung khuon Map-event giong 'budget.alerts' -> NotificationListener gui push
                eventPublisher.publishEvent(Map.of(
                        "topic", "wallet.negative",
                        "userId", userId,
                        "walletName", wallet.getName(),
                        "overAmount", vnd(wallet.getBalance().negate())));
            }
        }

        TransactionEntity entity = TransactionEntity.builder()
                .userId(userId)
                .walletId(request.getWalletId())
                .categoryId(resolveCategoryId(request.getCategoryId(), request.getCategoryName()))
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

    private static String vnd(BigDecimal value) {
        return String.format(java.util.Locale.forLanguageTag("vi-VN"), "%,.0f đ", value);
    }

    private void revertWalletBalance(TransactionEntity entity) {
        // Không try/catch: nếu revert lỗi thì để @Transactional rollback, tránh lệch số dư âm thầm
        walletService.updateBalance(entity.getWalletId(), entity.getUserId(), entity.getAmount(), entity.getType() == CategoryType.EXPENSE);
    }

    private void applyWalletBalance(Long walletId, java.math.BigDecimal amount,
                                     CategoryType type, String currency, String desc) {
        // Không try/catch: nếu lỗi thì để @Transactional rollback
        walletService.updateBalance(walletId, null, amount, type != CategoryType.EXPENSE);
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
                .categoryName(entity.getCategoryId() == null ? null :
                        categoryRepository.findById(entity.getCategoryId())
                                .map(com.fpm2025.wallet_service.entity.CategoryEntity::getName).orElse(null))
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

