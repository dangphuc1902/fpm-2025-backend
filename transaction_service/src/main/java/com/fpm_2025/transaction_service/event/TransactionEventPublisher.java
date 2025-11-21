package com.fpm_2025.transaction_service.event;

import com.fpm_2025.transaction_service.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes transaction events to RabbitMQ
 * 
 * Events:
 * - transaction.created → wallet-service updates balance
 * - transaction.updated → wallet-service adjusts balance
 * - transaction.deleted → wallet-service reverts balance
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    private static final String TRANSACTION_EXCHANGE = "transaction.exchange";

    public void publishTransactionCreated(TransactionCreatedEvent event) {
        log.info("Publishing transaction.created event: transactionId={}", 
            event.getTransactionId());
        
        try {
            rabbitTemplate.convertAndSend(
                TRANSACTION_EXCHANGE,
                "transaction.created",
                event
            );
            
            log.info("Transaction created event published successfully");
        } catch (Exception e) {
            log.error("Failed to publish transaction created event", e);
            // TODO: Implement retry logic or dead letter queue handling
        }
    }

    public void publishTransactionUpdated(TransactionUpdatedEvent event) {
        log.info("Publishing transaction.updated event: transactionId={}", 
            event.getTransactionId());
        
        try {
            rabbitTemplate.convertAndSend(
                TRANSACTION_EXCHANGE,
                "transaction.updated",
                event
            );
            
            log.info("Transaction updated event published successfully");
        } catch (Exception e) {
            log.error("Failed to publish transaction updated event", e);
        }
    }

    public void publishTransactionDeleted(TransactionDeletedEvent event) {
        log.info("Publishing transaction.deleted event: transactionId={}",
            event.getTransactionId());
        
        try {
            rabbitTemplate.convertAndSend(
                TRANSACTION_EXCHANGE,
                "transaction.deleted",
                event
            );
            
            log.info("Transaction deleted event published successfully");	    
        } catch (Exception e) {
            log.error("Failed to publish transaction deleted event", e);
        }
    }
}