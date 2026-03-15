package com.fpm_2025.reportingservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaTransactionConsumer {

    @KafkaListener(topics = {"transaction.created", "transaction.updated", "transaction.deleted"}, groupId = "reporting-group")
    @CacheEvict(value = "dashboard", allEntries = true)
    public void consumeTransactionEvent(Object event) {
        log.info("Received transaction event, evicting dashboard cache...");
    }
}
