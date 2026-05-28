package com.fpm2025.transaction_service.event.consumer;

import com.fpm2025.domain.dto.request.TransactionRequest;
import com.fpm2025.domain.dto.response.WalletResponse;
import com.fpm2025.domain.enums.CategoryType;
import com.fpm2025.domain.event.ParsedNotificationEvent;
import com.fpm2025.transaction_service.service.TransactionService;
import com.fpm_2025.wallet_service.service.WalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Consumer lắng nghe sự kiện 'notification.parsed' từ notification-service.
 * Tiến hành tạo giao dịch tự động.
 */
@Component
@Slf4j
public class ParsedNotificationConsumer {

    private final TransactionService transactionService;
    private final WalletService walletService;

    public ParsedNotificationConsumer(
            TransactionService transactionService,
            WalletService walletService) {
        this.transactionService = transactionService;
        this.walletService = walletService;
    }

    @KafkaListener(topics = "notification.parsed", groupId = "transaction-group")
    @EventListener
    @Async
    public void handleNotificationParsed(ParsedNotificationEvent event) {
        log.info("[Local/Kafka Received] ParsedNotificationEvent: userId={} amount={} type={}",
                event.getUserId(), event.getAmount(), event.getType());

        try {
            // 1 Tìm wallet phù hợp (heuristic: tìm ví có tên giống account hoặc lấy ví mặc định đầu tiên)
            Long walletId = resolveWalletId(event.getUserId(), event.getAccount(), event.getBankName());
            
            if (walletId == null) {
                log.warn("Could not resolve wallet for userId={} account={}. Skipping auto-transaction.",
                        event.getUserId(), event.getAccount());
                return;
            }

            // 2 Tạo TransactionRequest
            TransactionRequest request = TransactionRequest.builder()
                    .walletId(walletId)
                    .amount(event.getAmount())
                    .currency("VND")
                    .type("INCOME".equalsIgnoreCase(event.getType()) ? CategoryType.INCOME : CategoryType.EXPENSE)
                    .categoryId(null)
                    .transactionDate(LocalDateTime.now())
                    .description(event.getNote() != null ? event.getNote() : "Giao dịch tự động từ " + event.getBankName())
                    .note("Ref: " + event.getTransactionRef())
                    .build();

            // 3 Gọi service tạo giao dịch
            transactionService.createTransaction(event.getUserId(), request);
            log.info("[Local/Kafka Processed] Auto-transaction created for userId={} walletId={}", event.getUserId(), walletId);

        } catch (Exception e) {
            log.error("[Local/Kafka Failed] Failed to process parsed notification for userId={}, error={}",
                    event.getUserId(), e.getMessage(), e);
        }
    }

    private Long resolveWalletId(Long userId, String account, String bankName) {
        try {
            List<WalletResponse> wallets = walletService.getUserActiveWallets(userId);

            if (wallets.isEmpty()) return null;

            for (var w : wallets) {
                if (account != null && !account.isEmpty() && w.getName().contains(account)) return w.getId();
                if (bankName != null && !bankName.isEmpty() && w.getName().toLowerCase().contains(bankName.toLowerCase())) return w.getId();
            }

            return wallets.get(0).getId();
        } catch (Exception e) {
            log.error("Failed to resolve wallet directly: {}", e.getMessage());
            return null;
        }
    }
}
