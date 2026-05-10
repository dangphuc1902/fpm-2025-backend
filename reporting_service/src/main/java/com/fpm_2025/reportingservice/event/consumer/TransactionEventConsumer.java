package com.fpm_2025.reportingservice.event.consumer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpm2025.domain.event.TransactionCreatedEvent;
import com.fpm_2025.reportingservice.entity.TransactionSummaryEntity;
import com.fpm_2025.reportingservice.repository.TransactionSummaryRepository;

@Service
public class TransactionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final TransactionSummaryRepository summaryRepository;
    private final org.springframework.cache.CacheManager cacheManager;

    public TransactionEventConsumer(ObjectMapper objectMapper, TransactionSummaryRepository summaryRepository,
                                    org.springframework.cache.CacheManager cacheManager) {
        this.objectMapper = objectMapper;
        this.summaryRepository = summaryRepository;
        this.cacheManager = cacheManager;
    }

    private void clearDashboardCache() {
        if (cacheManager != null && cacheManager.getCache("dashboard") != null) {
            cacheManager.getCache("dashboard").clear();
            log.info("Reporting Dashboard cache cleared due to transaction event");
        }
    }

    @KafkaListener(topics = "transaction.created", groupId = "reporting-group")
    public void consumeTransactionCreated(TransactionCreatedEvent event) {
        try {
            log.info("Kafka: Received transaction.created event for userId={}", event.getUserId());

            // Convert Instant to LocalDateTime for period formatting
            LocalDateTime date = LocalDateTime.ofInstant(event.getTimestamp(), ZoneId.systemDefault());
            String period = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));

            TransactionSummaryEntity summary = summaryRepository.findByUserIdAndPeriod(event.getUserId(), period)
                    .orElse(TransactionSummaryEntity.builder()
                            .userId(event.getUserId())
                            .period(period)
                            .totalIncome(BigDecimal.ZERO)
                            .totalExpense(BigDecimal.ZERO)
                            .build());

            if ("INCOME".equalsIgnoreCase(event.getType())) {
                summary.setTotalIncome(summary.getTotalIncome().add(event.getAmount()));
            } else if ("EXPENSE".equalsIgnoreCase(event.getType())) {
                summary.setTotalExpense(summary.getTotalExpense().add(event.getAmount()));
            }

            summaryRepository.save(summary);
            log.info("Kafka: Updated reporting summary for userId={} period={}", event.getUserId(), period);

            clearDashboardCache();

        } catch (Exception e) {
            log.error("Kafka: Failed to process transaction.created event", e);
        }
    }

    @KafkaListener(topics = "transaction.updated", groupId = "reporting-group")
    public void consumeTransactionUpdated(String message) {
        log.info("Kafka: Received transaction.updated event, clearing cache...");
        clearDashboardCache();
    }

    @KafkaListener(topics = "transaction.deleted", groupId = "reporting-group")
    public void consumeTransactionDeleted(String message) {
        log.info("Kafka: Received transaction.deleted event, clearing cache...");
        clearDashboardCache();
    }
}
