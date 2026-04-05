package com.fpm2025.transaction_service.service;

import com.fpm2025.transaction_service.dto.TransactionRequest;
import com.fpm2025.transaction_service.dto.TransactionResponse;
import com.fpm2025.transaction_service.dto.UpdateTransactionRequest;
import com.fpm2025.transaction_service.entity.TransactionEntity;
import com.fpm2025.transaction_service.entity.enums.TransactionStatus;
import com.fpm2025.transaction_service.entity.enums.TransactionType;
import com.fpm2025.transaction_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import net.devh.boot.grpc.client.inject.GrpcClient;
import com.fpm2025.protocol.wallet.WalletGrpcServiceGrpc;
import com.fpm2025.protocol.wallet.UpdateBalanceRequest;
import com.fpm2025.protocol.wallet.WalletResponse;
import com.fpm2025.protocol.common.Money;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RabbitTemplate rabbitTemplate;

    @GrpcClient("wallet-service")
    private WalletGrpcServiceGrpc.WalletGrpcServiceBlockingStub walletGrpcStub;

    // =====================================================================
    // CREATE
    // =====================================================================

    @Transactional
    public TransactionResponse createTransaction(Long userId, TransactionRequest request) {
        log.info("Creating transaction for user {} in wallet {}", userId, request.getWalletId());

        // 1. [gRPC] Gọi Wallet Service để trừ/cộng tiền (đồng bộ)
        try {
            Money money = Money.newBuilder()
                    .setAmount(request.getAmount().doubleValue())
                    .setCurrency(request.getCurrency())
                    .build();

            UpdateBalanceRequest balanceRequest = UpdateBalanceRequest.newBuilder()
                    .setWalletId(request.getWalletId())
                    .setAmount(money)
                    .setOperation(request.getType() == TransactionType.EXPENSE ? "SUBTRACT" : "ADD")
                    .setDescription(request.getDescription() != null ? request.getDescription() : "")
                    .build();

            WalletResponse walletResponse = walletGrpcStub.updateBalance(balanceRequest);
            log.info("gRPC: Balance updated successfully for wallet: {}", walletResponse.getId());
        } catch (Exception e) {
            log.error("gRPC: Failed to update balance in Wallet Service", e);
            throw new RuntimeException("Failed to update wallet balance: " + e.getMessage());
        }

        // 2. [Database] Lưu transaction
        TransactionEntity entity = TransactionEntity.builder()
                .userId(userId)
                .walletId(request.getWalletId())
                .categoryId(request.getCategoryId())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                .type(request.getType())
                .transactionDate(request.getTransactionDate())
                .description(request.getDescription())
                .note(request.getNote())
                .location(request.getLocation())
                .isRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false)
                .status(TransactionStatus.COMPLETED)
                .build();

        TransactionEntity saved = transactionRepository.save(entity);

        // 3. [Kafka] Publish event cho Reporting Service
        publishKafkaEvent("transaction.created", userId, saved);

        // 4. [RabbitMQ] Gửi tác vụ thông báo
        sendNotification(userId, request.getType(), request.getAmount(), request.getCurrency());

        return mapToResponse(saved);
    }

    // =====================================================================
    // READ — Get by ID
    // =====================================================================

    public TransactionResponse getTransaction(Long userId, Long transactionId) {
        TransactionEntity entity = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new RuntimeException(
                        "Transaction not found or access denied: id=" + transactionId));
        return mapToResponse(entity);
    }

    // =====================================================================
    // READ — List by Wallet (paged)
    // =====================================================================

    public Page<TransactionResponse> getTransactionsByWallet(Long userId, Long walletId, int page, int size) {
        Page<TransactionEntity> transactions = transactionRepository.findByWalletId(
                walletId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate")));
        return transactions.map(this::mapToResponse);
    }

    // =====================================================================
    // READ — List by User với filter (date, category, type, walletId)
    // =====================================================================

    public Page<TransactionResponse> listTransactions(
            Long userId,
            Long walletId,
            Long categoryId,
            TransactionType type,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page, int size) {

        log.info("Listing transactions for user={} walletId={} categoryId={} type={} from={} to={}",
                userId, walletId, categoryId, type, startDate, endDate);

        Page<TransactionEntity> result = transactionRepository.findByFilters(
                userId, walletId, categoryId, type, startDate, endDate,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate")));

        return result.map(this::mapToResponse);
    }

    // =====================================================================
    // UPDATE
    // =====================================================================

    @Transactional
    public TransactionResponse updateTransaction(Long userId, Long transactionId, UpdateTransactionRequest request) {
        log.info("Updating transaction {} for user {}", transactionId, userId);

        TransactionEntity entity = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new RuntimeException(
                        "Transaction not found or access denied: id=" + transactionId));

        // Nếu có thay đổi amount/type → cần revert balance cũ và apply balance mới
        boolean balanceChanged = (request.getAmount() != null && !request.getAmount().equals(entity.getAmount()))
                || (request.getType() != null && request.getType() != entity.getType());

        if (balanceChanged) {
            // Revert balance: hoàn lại giao dịch cũ
            revertWalletBalance(entity);
            // Apply balance mới
            applyWalletBalance(
                    entity.getWalletId(),
                    request.getAmount() != null ? request.getAmount() : entity.getAmount(),
                    request.getType() != null ? request.getType() : entity.getType(),
                    request.getCurrency() != null ? request.getCurrency() : entity.getCurrency(),
                    "Update transaction #" + transactionId);
        }

        // Áp dụng các thay đổi (chỉ field không null)
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

        // Kafka: publish updated event
        publishKafkaEvent("transaction.updated", userId, updated);

        log.info("Transaction {} updated successfully", transactionId);
        return mapToResponse(updated);
    }

    // =====================================================================
    // DELETE
    // =====================================================================

    @Transactional
    public void deleteTransaction(Long userId, Long transactionId) {
        log.info("Deleting transaction {} for user {}", transactionId, userId);

        TransactionEntity entity = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new RuntimeException(
                        "Transaction not found or access denied: id=" + transactionId));

        // Revert balance khi delete
        revertWalletBalance(entity);

        transactionRepository.delete(entity);

        // Kafka: publish deleted event để reporting-service cập nhật
        try {
            kafkaTemplate.send("transaction.deleted", String.valueOf(userId),
                    java.util.Map.of(
                            "transactionId", transactionId,
                            "userId", userId,
                            "walletId", entity.getWalletId(),
                            "amount", entity.getAmount(),
                            "type", entity.getType().name(),
                            "deletedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)));
            log.info("Kafka: Published transaction.deleted event for txId: {}", transactionId);
        } catch (Exception e) {
            log.error("Kafka: Failed to publish transaction.deleted event", e);
        }

        // RabbitMQ: thông báo user
        try {
            String msg = String.format("User %d deleted transaction #%d", userId, transactionId);
            rabbitTemplate.convertAndSend("notification.exchange", "notification.routing.key", msg);
        } catch (Exception e) {
            log.error("RabbitMQ: Failed to send notification for delete", e);
        }
    }

    // =====================================================================
    // ATTACHMENTS
    // =====================================================================

    @Transactional
    public com.fpm2025.transaction_service.entity.TransactionAttachmentEntity uploadAttachment(
            Long userId, Long transactionId, org.springframework.web.multipart.MultipartFile file) {
        
        log.info("Uploading attachment for transaction {} user {}", transactionId, userId);
        
        // Check transaction ownership
        TransactionEntity tx = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new RuntimeException("Transaction not found or access denied"));

        // Simulate file upload logic (Real storage: S3, Cloudinary, etc.)
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
        log.info("Deleting attachment {} for transaction {}", attachmentId, transactionId);
        
        // Check transaction ownership
        transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new RuntimeException("Transaction not found or access denied"));
                
        attachmentRepository.deleteById(attachmentId);
    }

    private final com.fpm2025.transaction_service.repository.TransactionAttachmentRepository attachmentRepository;

    // =====================================================================
    // gRPC Helper Methods (dùng bởi TransactionServiceGrpcImpl)
    // =====================================================================

    public TransactionEntity findById(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
    }

    public Page<TransactionEntity> findByWalletIdRaw(Long walletId, int page, int size) {
        return transactionRepository.findByWalletId(walletId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate")));
    }

    public java.util.List<TransactionEntity> findByUserAndDateRange(
            Long userId, LocalDateTime startDate, LocalDateTime endDate, java.util.List<Long> walletIds) {
        return transactionRepository.findByUserIdAndDateRangeAndWallets(userId, startDate, endDate, walletIds);
    }

    public java.math.BigDecimal sumExpense(Long userId, LocalDateTime startDate, LocalDateTime endDate, Long categoryId) {
        return transactionRepository.sumExpenseByUserAndDateRange(userId, startDate, endDate, categoryId);
    }

    // =====================================================================
    // Private Helpers
    // =====================================================================

    private void revertWalletBalance(TransactionEntity entity) {
        // Revert: nếu EXPENSE thì cộng lại, nếu INCOME thì trừ đi
        String revertOp = entity.getType() == TransactionType.EXPENSE ? "ADD" : "SUBTRACT";
        try {
            Money money = Money.newBuilder()
                    .setAmount(entity.getAmount().doubleValue())
                    .setCurrency(entity.getCurrency())
                    .build();
            UpdateBalanceRequest req = UpdateBalanceRequest.newBuilder()
                    .setWalletId(entity.getWalletId())
                    .setAmount(money)
                    .setOperation(revertOp)
                    .setDescription("Revert transaction #" + entity.getId())
                    .build();
            walletGrpcStub.updateBalance(req);
            log.info("gRPC: Reverted balance for wallet {} (op={})", entity.getWalletId(), revertOp);
        } catch (Exception e) {
            log.error("gRPC: Failed to revert wallet balance", e);
            throw new RuntimeException("Failed to revert wallet balance: " + e.getMessage());
        }
    }

    private void applyWalletBalance(Long walletId, java.math.BigDecimal amount,
                                     TransactionType type, String currency, String desc) {
        String op = type == TransactionType.EXPENSE ? "SUBTRACT" : "ADD";
        try {
            Money money = Money.newBuilder()
                    .setAmount(amount.doubleValue())
                    .setCurrency(currency)
                    .build();
            UpdateBalanceRequest req = UpdateBalanceRequest.newBuilder()
                    .setWalletId(walletId)
                    .setAmount(money)
                    .setOperation(op)
                    .setDescription(desc)
                    .build();
            walletGrpcStub.updateBalance(req);
            log.info("gRPC: Applied balance for wallet {} (op={})", walletId, op);
        } catch (Exception e) {
            log.error("gRPC: Failed to apply wallet balance", e);
            throw new RuntimeException("Failed to apply wallet balance: " + e.getMessage());
        }
    }

    private void publishKafkaEvent(String topic, Long userId, TransactionEntity saved) {
        try {
            kafkaTemplate.send(topic, String.valueOf(userId), mapToResponse(saved));
            log.info("Kafka: Published {} event for txId: {}", topic, saved.getId());
        } catch (Exception e) {
            log.error("Kafka: Failed to publish {} event", topic, e);
        }
    }

    private void sendNotification(Long userId, TransactionType type, java.math.BigDecimal amount, String currency) {
        try {
            String msg = String.format("User %d has a new %s transaction of %s %s",
                    userId, type, amount, currency);
            rabbitTemplate.convertAndSend("notification.exchange", "notification.routing.key", msg);
            log.info("RabbitMQ: Sent notification task to queue");
        } catch (Exception e) {
            log.error("RabbitMQ: Failed to send notification task", e);
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
                .transactionDate(entity.getTransactionDate())
                .description(entity.getDescription())
                .note(entity.getNote())
                .location(entity.getLocation())
                .isRecurring(entity.getIsRecurring())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Tự động xử lý notification từ ngân hàng (đã parse) để tạo transaction.
     */
    @Transactional
    public TransactionResponse processBankNotification(Long userId, com.fpm2025.transaction_service.dto.BankNotificationRequest request) {
        log.info("Processing bank notification for userId={}: bank={}, amount={}", 
                userId, request.getBankName(), request.getAmount());

        // 1. Tìm walletId phù hợp
        Long walletId = resolveWalletIdByAccount(userId, request.getAccount(), request.getBankName());
        
        if (walletId == null) {
            throw new RuntimeException("Could not identify target wallet for account: " + request.getAccount());
        }

        // 2. Build TransactionRequest
        TransactionRequest txRequest = TransactionRequest.builder()
                .walletId(walletId)
                .amount(request.getAmount())
                .currency("VND")
                .type("INCOME".equalsIgnoreCase(request.getType()) ? TransactionType.INCOME : TransactionType.EXPENSE)
                .transactionDate(LocalDateTime.now())
                .description(request.getNote() != null ? request.getNote() : "Auto: " + request.getBankName())
                .note("Ref: " + request.getTransactionRef())
                .build();

        // 3. Sử dụng logic tạo transaction chính để đảm bảo cập nhật ví
        return createTransaction(userId, txRequest);
    }

    private Long resolveWalletIdByAccount(Long userId, String account, String bankName) {
        try {
            com.fpm2025.protocol.wallet.UserWalletsRequest req = com.fpm2025.protocol.wallet.UserWalletsRequest.newBuilder()
                    .setUserId(userId)
                    .setActiveOnly(true)
                    .build();
            com.fpm2025.protocol.wallet.WalletsResponse response = walletGrpcStub.getWalletsByUserId(req);

            if (response.getWalletsCount() == 0) return null;

            // Heuristic: tìm ví có tên khớp hoặc lấy ví đầu tiên
            for (var w : response.getWalletsList()) {
                if (account != null && !account.isEmpty() && w.getName().contains(account)) return w.getId();
                if (bankName != null && !bankName.isEmpty() && w.getName().toLowerCase().contains(bankName.toLowerCase())) return w.getId();
            }

            return response.getWalletsList().get(0).getId();
        } catch (Exception e) {
            log.error("Failed to resolve wallet via gRPC for bank notification", e);
            return null;
        }
    }
}
