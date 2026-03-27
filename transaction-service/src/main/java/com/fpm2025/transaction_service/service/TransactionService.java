package com.fpm2025.transaction_service.service;

import com.fpm2025.transaction_service.dto.TransactionRequest;
import com.fpm2025.transaction_service.dto.TransactionResponse;
import com.fpm2025.transaction_service.entity.TransactionEntity;
import com.fpm2025.transaction_service.entity.enums.TransactionStatus;
import com.fpm2025.transaction_service.entity.enums.TransactionType;
import com.fpm2025.transaction_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import net.devh.boot.grpc.client.inject.GrpcClient;
import com.fpm2025.protocol.wallet.WalletGrpcServiceGrpc;
import com.fpm2025.protocol.wallet.UpdateBalanceRequest;
import com.fpm2025.protocol.wallet.WalletResponse;
import com.fpm2025.protocol.common.Money;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RabbitTemplate rabbitTemplate;

    @GrpcClient("wallet-service")
    private WalletGrpcServiceGrpc.WalletGrpcServiceBlockingStub walletGrpcStub;

    @Transactional
    public TransactionResponse createTransaction(Long userId, TransactionRequest request) {
        log.info("Creating transaction for user {} in wallet {}", userId, request.getWalletId());
        
        // 1. [gRPC] Call Wallet Service to update balance synchronously
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
            // Non-blocking for demonstration, but typically this throws an exception
        }

        // 2. [Database] Save transaction entity
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

        // 3. [Kafka] Publish event to Kafka for Reporting Service / Audit logs
        try {
            kafkaTemplate.send("transaction.created", String.valueOf(userId), mapToResponse(saved));
            log.info("Kafka: Published transaction.created event for txId: {}", saved.getId());
        } catch (Exception e) {
            log.error("Kafka: Failed to publish transaction.created event", e);
        }

        // 4. [RabbitMQ] Send task to RabbitMQ for Notification Service
        try {
            String notificationMsg = String.format("User %d has a new %s transaction of %s %s", 
                                                    userId, request.getType(), request.getAmount(), request.getCurrency());
            rabbitTemplate.convertAndSend("notification.exchange", "notification.routing.key", notificationMsg);
            log.info("RabbitMQ: Sent notification task to queue");
        } catch (Exception e) {
            log.error("RabbitMQ: Failed to send notification task", e);
        }

        // 5. [REST API] Return response to mobile/frontend
        return mapToResponse(saved);
    }

    public Page<TransactionResponse> getTransactionsByWallet(Long userId, Long walletId, int page, int size) {
        Page<TransactionEntity> transactions = transactionRepository.findByWalletId(walletId, PageRequest.of(page, size));
        return transactions.map(this::mapToResponse);
    }

    public TransactionResponse getTransaction(Long userId, Long transactionId) {
        TransactionEntity entity = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        if (!entity.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to transaction");
        }
        
        return mapToResponse(entity);
    }

    private TransactionResponse mapToResponse(TransactionEntity entity) {
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
}
