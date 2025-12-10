package com.fpm_2025.reportingservice.event.consumer;

import com.fpm2025.domain.transaction.event.TransactionCreatedEvent;
import com.fpm2025.messaging.constant.KafkaTopics;
import com.fpm2025.messaging.kafka.consumer.BaseEventConsumer;
import com.fpm2025.reporting.service.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventConsumer extends BaseEventConsumer<TransactionCreatedEvent> {

    private final StatisticsService statisticsService;

    @Override
    @KafkaListener(
            topics = KafkaTopics.TRANSACTION_CREATED,
            groupId = "reporting-service-group"
    )
    public void processEvent(TransactionCreatedEvent event) {
        log.info("Processing transaction created event: transactionId={}", event.getTransactionId());
        
        // Update statistics
        statisticsService.updateStatistics(event);
        
        // Update monthly summary
        statisticsService.updateMonthlySummary(event);
    }

    @Override
    protected String getEventType() {
        return "TransactionCreated";
    }

    @Override
    protected Class<TransactionCreatedEvent> getEventClass() {
        return TransactionCreatedEvent.class;
    }

    @KafkaListener(
            topics = KafkaTopics.TRANSACTION_CREATED,
            groupId = "reporting-service-group"
    )
    public void consume(String message, Acknowledgment acknowledgment) {
        handleEvent(message, acknowledgment);
    }
}