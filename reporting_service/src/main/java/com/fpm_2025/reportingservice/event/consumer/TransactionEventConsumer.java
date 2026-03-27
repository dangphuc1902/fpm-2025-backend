package com.fpm_2025.reportingservice.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpm_2025.reportingservice.dto.TransactionEventDto;
import com.fpm_2025.reportingservice.entity.TransactionSummaryEntity;
import com.fpm_2025.reportingservice.repository.TransactionSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class TransactionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final TransactionSummaryRepository summaryRepository;

    public TransactionEventConsumer(ObjectMapper objectMapper, TransactionSummaryRepository summaryRepository) {
        this.objectMapper = objectMapper;
        this.summaryRepository = summaryRepository;
    }

    @KafkaListener(topics = "transaction.created", groupId = "reporting-group")
    public void consumeTransactionCreated(String message) {
        try {
            log.info("Kafka: Received transaction.created event: {}", message);
            TransactionEventDto event = objectMapper.readValue(message, TransactionEventDto.class);

            // Period string like "2025-03"
            String period = event.getTransactionDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));

            // Get or create summary record for this user/period
            TransactionSummaryEntity summary = summaryRepository.findByUserIdAndPeriod(event.getUserId(), period)
                    .orElse(TransactionSummaryEntity.builder()
                            .userId(event.getUserId())
                            .period(period)
                            .totalIncome(BigDecimal.ZERO)
                            .totalExpense(BigDecimal.ZERO)
                            .build());

            // Accumulate income or expense
            if ("INCOME".equalsIgnoreCase(event.getType())) {
                summary.setTotalIncome(summary.getTotalIncome().add(event.getAmount()));
            } else if ("EXPENSE".equalsIgnoreCase(event.getType())) {
                summary.setTotalExpense(summary.getTotalExpense().add(event.getAmount()));
            }

            summaryRepository.save(summary);
            log.info("Kafka: Updated reporting summary for userId={} period={}", event.getUserId(), period);

        } catch (Exception e) {
            log.error("Kafka: Failed to process transaction.created event", e);
        }
    }
}
