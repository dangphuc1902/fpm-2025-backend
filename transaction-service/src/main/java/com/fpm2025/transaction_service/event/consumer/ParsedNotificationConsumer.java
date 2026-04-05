package com.fpm2025.transaction_service.event.consumer;

import com.fpm2025.transaction_service.dto.TransactionRequest;
import com.fpm2025.transaction_service.entity.enums.TransactionType;
import com.fpm2025.transaction_service.event.model.ParsedNotificationEvent;
import com.fpm2025.transaction_service.service.TransactionService;
import com.fpm2025.protocol.wallet.UserWalletsRequest;
import com.fpm2025.protocol.wallet.WalletGrpcServiceGrpc;
import com.fpm2025.protocol.wallet.WalletsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Kafka Consumer lắng nghe sự kiện 'notification.parsed' từ notification-service.
 * Tiến hành tạo giao dịch tự động.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ParsedNotificationConsumer {

    private final TransactionService transactionService;

    @GrpcClient("wallet-service")
    private WalletGrpcServiceGrpc.WalletGrpcServiceBlockingStub walletStub;

    @KafkaListener(topics = "notification.parsed", groupId = "transaction-group")
    public void handleNotificationParsed(ParsedNotificationEvent event) {
        log.info("[Kafka Received] ParsedNotificationEvent: userId={} amount={} type={}",
                event.getUserId(), event.getAmount(), event.getType());

        try {
            // 1️ Tìm wallet phù hợp (heuristic: tìm ví có tên giống account hoặc lấy ví mặc định đầu tiên)
            Long walletId = resolveWalletId(event.getUserId(), event.getAccount(), event.getBankName());
            
            if (walletId == null) {
                log.warn("[Kafka] Could not resolve wallet for userId={} account={}. Skipping auto-transaction.",
                        event.getUserId(), event.getAccount());
                return;
            }

            // 2️ Tạo TransactionRequest
            TransactionRequest request = TransactionRequest.builder()
                    .walletId(walletId)
                    .amount(event.getAmount())
                    .currency("VND")
                    .type("INCOME".equalsIgnoreCase(event.getType()) ? TransactionType.INCOME : TransactionType.EXPENSE)
                    .categoryId(null) // Sẽ do AI hoặc user phân loại sau, hiện tại để null
                    .transactionDate(LocalDateTime.now()) // Có thể lấy từ event.getTransactionTime() nếu parse được chuẩn
                    .description(event.getNote() != null ? event.getNote() : "Giao dịch tự động từ " + event.getBankName())
                    .note("Ref: " + event.getTransactionRef())
                    .build();

            // 3️ Gọi service tạo giao dịch
            transactionService.createTransaction(event.getUserId(), request);
            log.info("[Kafka Processed] Auto-transaction created for userId={} walletId={}", event.getUserId(), walletId);

        } catch (Exception e) {
            log.error("[Kafka Failed] Failed to process parsed notification for userId={}, error={}",
                    event.getUserId(), e.getMessage(), e);
        }
    }

    private Long resolveWalletId(Long userId, String account, String bankName) {
        try {
            UserWalletsRequest request = UserWalletsRequest.newBuilder()
                    .setUserId(userId)
                    .setActiveOnly(true)
                    .build();
            WalletsResponse response = walletStub.getWalletsByUserId(request);

            if (response.getWalletsCount() == 0) return null;

            // Heuristic 1: Tìm ví có tên chứa bankName hoặc account
            for (var w : response.getWalletsList()) {
                if (account != null && !account.isEmpty() && w.getName().contains(account)) return w.getId();
                if (bankName != null && !bankName.isEmpty() && w.getName().toLowerCase().contains(bankName.toLowerCase())) return w.getId();
            }

            // Heuristic 2: Lấy ví đầu tiên (Thường là ví mặc định của user)
            return response.getWalletsList().get(0).getId();
        } catch (Exception e) {
            log.error("Failed to resolve wallet via gRPC: {}", e.getMessage());
            return null;
        }
    }
}
