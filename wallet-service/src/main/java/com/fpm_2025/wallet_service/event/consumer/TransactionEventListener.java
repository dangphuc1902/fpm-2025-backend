package com.fpm_2025.wallet_service.event.consumer;

import com.fpm_2025.wallet_service.config.RabbitMQConfig;
import com.fpm_2025.wallet_service.event.model.TransactionCreatedEvent;
import com.fpm_2025.wallet_service.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Consumer lắng nghe TransactionCreatedEvent từ transaction_service.
 * Khi nhận được event, tiến hành cập nhật lại số dư của ví tương ứng.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventListener {
	@Autowired
    private WalletService walletService;
    Logger logger  = LoggerFactory.getLogger(TransactionEventListener.class);
    @RabbitListener(queues = RabbitMQConfig.TRANSACTION_CREATED_QUEUE)
    public void handleTransactionCreated(TransactionCreatedEvent event) {
    	logger.info("[Event Received] TransactionCreatedEvent: transactionId={} walletId={} amount={}",
                event.getTransactionId(), event.getWalletId(), event.getAmount());

        try {
            // Logic cập nhật số dư ví khi có giao dịch mới
            walletService.updateBalanceFromTransaction(event);

            logger.info("[Event Processed] Wallet updated successfully for walletId={}", event.getWalletId());
        } catch (Exception e) {
        	logger.error("[Event Failed] TransactionCreatedEvent: walletId={}, error={}", event.getWalletId(), e.getMessage(), e);
            throw e; // gửi về DLQ
        }
    }
}
